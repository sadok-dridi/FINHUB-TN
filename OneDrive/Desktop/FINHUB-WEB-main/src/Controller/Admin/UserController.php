<?php

namespace App\Controller\Admin;

use App\Entity\User;
use App\Repository\UserRepository;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Annotation\Route;
use Symfony\Component\Security\Http\Attribute\IsGranted;
use Symfony\Contracts\HttpClient\HttpClientInterface;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\Mailer\MailerInterface;
use Symfony\Bridge\Twig\Mime\TemplatedEmail;
use Symfony\Component\Mime\Address;
use App\Service\MarketService;

#[IsGranted('ROLE_ADMIN')]
class UserController extends AbstractController
{
    #[Route('/admin/users', name: 'app_admin_users')]
    public function index(Request $request, UserRepository $userRepository): Response
    {
        $sort = $request->query->get('sort', 'name');
        $direction = strtolower($request->query->get('direction', 'asc')) === 'desc' ? 'DESC' : 'ASC';
        $page = max(1, $request->query->getInt('page', 1));
        $perPage = 12;

        $allowedSorts = ['name', 'role', 'trust'];
        if (!in_array($sort, $allowedSorts, true)) {
            $sort = 'name';
        }

        $qb = $userRepository->createQueryBuilder('u');

        switch ($sort) {
            case 'role':
                $qb
                    ->addSelect("CASE WHEN UPPER(u.role) IN ('ADMIN', 'ROLE_ADMIN') THEN 0 ELSE 1 END AS HIDDEN roleSort")
                    ->orderBy('roleSort', $direction)
                    ->addOrderBy('u.full_name', 'ASC');
                break;

            case 'trust':
                $qb
                    ->orderBy('u.trust_score', $direction)
                    ->addOrderBy('u.full_name', 'ASC');
                break;

            case 'name':
            default:
                $qb->orderBy('u.full_name', $direction);
                break;
        }

        $totalUsers = (int) $userRepository->createQueryBuilder('u')
            ->select('COUNT(u.id)')
            ->getQuery()
            ->getSingleScalarResult();

        $totalPages = max(1, (int) ceil($totalUsers / $perPage));
        $page = min($page, $totalPages);

        $users = $qb
            ->setFirstResult(($page - 1) * $perPage)
            ->setMaxResults($perPage)
            ->getQuery()
            ->getResult();

        return $this->render('admin/users.html.twig', [
            'users' => $users,
            'currentPage' => $page,
            'totalPages' => $totalPages,
            'totalUsers' => $totalUsers,
            'sort' => $sort,
            'direction' => strtolower($direction),
        ]);
    }

    #[Route('/admin/users/{email}/promote', name: 'app_admin_user_promote', methods: ['POST'])]
    public function promote(string $email, Request $request, HttpClientInterface $client, MailerInterface $mailer): JsonResponse
    {
        $token = $request->getSession()->get('api_token');
        
        if (!$token) {
            return new JsonResponse(['success' => false, 'message' => 'Authentication token missing. Please login again.'], 401);
        }

        try {
            // Call the python API to generate the admin invitation token and link
            $response = $client->request('POST', 'https://api.finhub.tn/admin/invite?email=' . urlencode($email), [
                'headers' => [
                    'Authorization' => 'Bearer ' . $token,
                    'Content-Type' => 'application/json',
                ],
            ]);

            if ($response->getStatusCode() >= 200 && $response->getStatusCode() < 300) {
                $data = $response->toArray();
                $inviteLink = $data['invite_link'] ?? null;

                if ($inviteLink) {
                    // Send the invite email with the API-generated link (exactly like the Java app)
                    $emailMessage = (new TemplatedEmail())
                        ->from(new Address('sadok.dridi.engineer@gmail.com', 'FINHUB Security'))
                        ->to($email)
                        ->subject('You have been invited as a FINHUB Administrator')
                        ->htmlTemplate('emails/admin_invite.html.twig')
                        ->context([
                            'invite_link' => $inviteLink,
                            'user' => ['email' => $email] // Fallback for the template since we might not have the full user object
                        ]);

                    try {
                        $mailer->send($emailMessage);
                        return new JsonResponse([
                            'success' => true, 
                            'message' => "An admin invitation email was sent successfully to:\n\n" . $email
                        ]);
                    } catch (\Exception $e) {
                        return new JsonResponse([
                            'success' => false, 
                            'message' => 'API link generated, but failed to send invitation email: ' . $e->getMessage()
                        ], 500);
                    }
                }

                return new JsonResponse(['success' => false, 'message' => 'Invalid response from API.'], 500);

            } else {
                $data = $response->toArray(false);
                $errorMsg = $data['detail'] ?? ($data['message'] ?? 'API Error');
                return new JsonResponse([
                    'success' => false, 
                    'message' => $errorMsg
                ], $response->getStatusCode());
            }
        } catch (\Exception $e) {
            return new JsonResponse([
                'success' => false, 
                'message' => 'Failed to connect to the API: ' . $e->getMessage()
            ], 500);
        }
    }

    #[Route('/admin/users/{id}', name: 'app_admin_user_details', methods: ['GET'])]
    public function details(int $id, UserRepository $userRepository, EntityManagerInterface $em): Response
    {
        $user = $userRepository->find($id);

        if (!$user) {
            throw $this->createNotFoundException('User not found');
        }

        if ($user->getRole() === 'ADMIN' || $user->getRole() === 'ROLE_ADMIN') {
            return $this->redirectToRoute('app_admin_users'); // Don't show details for admins
        }

        $wallet = $user->getWallet();
        $sparklineData = [];
        $labels = [];

        if ($wallet) {
            $transactions = $em->getRepository(\App\Entity\WalletTransaction::class)
                ->findBy(['wallet' => $wallet], ['created_at' => 'DESC'], 20);

            $runningBalance = (float) $wallet->getBalance();
            $limit = count($transactions);

            for ($i = 0; $i < $limit; $i++) {
                array_unshift($sparklineData, $runningBalance);
                array_unshift($labels, $limit - $i);

                $tx = $transactions[$i];
                $type = strtoupper($tx->getType());
                $amount = (float) $tx->getAmount();

                if (in_array($type, ['CREDIT', 'DEPOSIT', 'TRANSFER_RECEIVED'])) {
                    $runningBalance -= $amount;
                } elseif (in_array($type, ['DEBIT', 'WITHDRAWAL', 'TRANSFER_SENT'])) {
                    $runningBalance += $amount;
                }
            }

            // Add the final point (which is the oldest computed balance)
            array_unshift($sparklineData, $runningBalance);
            array_unshift($labels, 0);

            // If there are no transactions, just show current balance as a flat line
            if (empty($sparklineData)) {
                $sparklineData = [$runningBalance, $runningBalance];
                $labels = [1, 2];
            }
        }

        return $this->render('admin/user_details.html.twig', [
            'targetUser' => $user,
            'sparklineData' => json_encode($sparklineData),
            'sparklineLabels' => json_encode($labels),
        ]);
    }

    #[Route('/admin/users/{id}/freeze', name: 'app_admin_user_freeze', methods: ['POST'])]
    public function toggleFreeze(int $id, UserRepository $userRepository, EntityManagerInterface $em): JsonResponse
    {
        $user = $userRepository->find($id);

        if (!$user) {
            return new JsonResponse(['success' => false, 'message' => 'User not found'], 404);
        }

        $wallet = $user->getWallet();
        if (!$wallet) {
            return new JsonResponse(['success' => false, 'message' => 'User has no wallet'], 400);
        }

        if ($wallet->getStatus() === 'FROZEN') {
            $wallet->setStatus('ACTIVE');
            $message = 'Wallet unfrozen successfully.';
            
            // Clear ledger flags if they exist (mirroring Java app behavior)
            try {
                $conn = $em->getConnection();
                $conn->executeStatement('DELETE FROM ledger_flags WHERE wallet_id = :walletId', ['walletId' => $wallet->getId()]);
            } catch (\Exception $e) {
                // Ignore if table doesn't exist
            }
        } else {
            // Check for Central Bank
            if ($user->getId() === 1 || $user->getEmail() === 'centralbank@finhub.tn') {
                return new JsonResponse(['success' => false, 'message' => 'Attempts to freeze Central Bank Wallet blocked.'], 400);
            }
            $wallet->setStatus('FROZEN');
            $message = 'Wallet frozen successfully.';
        }

        $em->flush();

        return new JsonResponse(['success' => true, 'message' => $message, 'newStatus' => $wallet->getStatus()]);
    }

    #[Route('/admin/users/{id}/delete', name: 'app_admin_user_delete', methods: ['POST'])]
    public function deleteUser(int $id, Request $request, UserRepository $userRepository, EntityManagerInterface $em, HttpClientInterface $client): JsonResponse
    {
        $targetUser = $userRepository->find($id);

        if (!$targetUser) {
            return new JsonResponse(['success' => false, 'message' => 'User not found'], 404);
        }

        // 1. Password Confirmation (Re-Auth)
        $data = json_decode($request->getContent(), true);
        $password = $data['password'] ?? '';
        
        /** @var User $adminUser */
        $adminUser = $this->getUser();
        $adminEmail = $adminUser->getEmail();

        if (empty($password)) {
            return new JsonResponse(['success' => false, 'message' => 'Password is required to delete a user.'], 400);
        }

        try {
            $loginResponse = $client->request('POST', 'https://api.finhub.tn/login', [
                'json' => [
                    'email' => $adminEmail,
                    'password' => $password,
                ],
            ]);

            if ($loginResponse->getStatusCode() !== 200) {
                return new JsonResponse(['success' => false, 'message' => 'Authentication Failed: Incorrect Admin Password.'], 403);
            }
        } catch (\Exception $e) {
            return new JsonResponse(['success' => false, 'message' => 'Failed to verify admin password: ' . $e->getMessage()], 500);
        }

        $wallet = $targetUser->getWallet();
        
        if ($wallet) {
            // Check for escrows
            $escrowRepo = $em->getRepository(\App\Entity\Escrow::class);
            $escrowsAsSender = $escrowRepo->findBy(['senderWallet' => $wallet, 'status' => 'PENDING']);
            $escrowsAsReceiver = $escrowRepo->findBy(['receiverWallet' => $wallet, 'status' => 'PENDING']);
            
            if (count($escrowsAsSender) > 0 || count($escrowsAsReceiver) > 0) {
                return new JsonResponse(['success' => false, 'message' => 'Cannot delete user: Active Escrow transactions pending.'], 400);
            }

            // Liquidation logic: Delete portfolio items
            $portfolioItems = $em->getRepository(\App\Entity\PortfolioItem::class)->findBy(['user' => $targetUser]);
            foreach ($portfolioItems as $item) {
                $em->remove($item);
            }
        }

        try {
            // Delete user on server via API
            $token = $request->getSession()->get('api_token');
            if ($token) {
                $deleteResponse = $client->request('DELETE', 'https://api.finhub.tn/admin/users/' . $id, [
                    'headers' => [
                        'Authorization' => 'Bearer ' . $token,
                        'Accept' => 'application/json',
                    ],
                ]);
                
                if ($deleteResponse->getStatusCode() >= 400 && $deleteResponse->getStatusCode() !== 404) {
                    // Ignore 404 if user already deleted on server
                    $body = $deleteResponse->getContent(false);
                    return new JsonResponse(['success' => false, 'message' => 'Server delete failed: ' . $body], 500);
                }
            }

            // Local Cascade Delete to bypass foreign key constraints manually
            $conn = $em->getConnection();
            $userId = $targetUser->getId();
            
            if ($wallet) {
                $walletId = $wallet->getId();
                // Wallet-dependent tables
                try { $conn->executeStatement('DELETE FROM virtual_cards WHERE wallet_id = ?', [$walletId]); } catch(\Exception $e){}
                try { $conn->executeStatement('DELETE FROM wallet_transactions WHERE wallet_id = ?', [$walletId]); } catch(\Exception $e){}
                try { $conn->executeStatement('DELETE FROM ledger_flags WHERE wallet_id = ?', [$walletId]); } catch(\Exception $e){}
                try { $conn->executeStatement('DELETE FROM escrows WHERE sender_wallet_id = ? OR receiver_wallet_id = ?', [$walletId, $walletId]); } catch(\Exception $e){}
            }

            // User-dependent tables
            try { $conn->executeStatement('DELETE FROM portfolio_items WHERE user_id = ?', [$userId]); } catch(\Exception $e){}
            try { $conn->executeStatement('DELETE FROM saved_contacts WHERE user_id = ?', [$userId]); } catch(\Exception $e){}
            try { $conn->executeStatement('DELETE FROM saved_contacts WHERE contact_email = ?', [$targetUser->getEmail()]); } catch(\Exception $e){}
            try { $conn->executeStatement('DELETE FROM system_alerts WHERE user_id = ?', [$userId]); } catch(\Exception $e){}
            try { $conn->executeStatement('DELETE FROM kyc_requests WHERE user_id = ?', [$userId]); } catch(\Exception $e){}
            try { $conn->executeStatement('DELETE FROM simulated_trades WHERE user_id = ?', [$userId]); } catch(\Exception $e){}
            
            // Support tickets & messages
            try {
                $stmt = $conn->prepare('SELECT id FROM support_tickets WHERE user_id = ?');
                $result = $stmt->executeQuery([$userId]);
                while ($row = $result->fetchAssociative()) {
                    $conn->executeStatement('DELETE FROM support_messages WHERE ticket_id = ?', [$row['id']]);
                }
                $conn->executeStatement('DELETE FROM support_tickets WHERE user_id = ?', [$userId]);
            } catch(\Exception $e){}

            try { $conn->executeStatement('DELETE FROM financial_profiles WHERE user_id = ?', [$userId]); } catch(\Exception $e){}
            if ($wallet) {
                try { $conn->executeStatement('DELETE FROM wallets WHERE user_id = ?', [$userId]); } catch(\Exception $e){}
            }

            // Finally, explicitly remove the user from the users_local table
            try { $conn->executeStatement('DELETE FROM users_local WHERE user_id = ?', [$userId]); } catch(\Exception $e){}

            return new JsonResponse(['success' => true, 'message' => 'User deleted successfully. Assets liquidated and transferred.']);

        } catch (\Exception $e) {
            return new JsonResponse(['success' => false, 'message' => 'Deletion Failed: ' . $e->getMessage()], 500);
        }
    }

    #[Route('/admin/users/{id}/portfolio', name: 'app_admin_user_portfolio', methods: ['GET'])]
    public function portfolio(int $id, UserRepository $userRepository, EntityManagerInterface $em, MarketService $marketService): Response
    {
        $targetUser = $userRepository->find($id);

        if (!$targetUser) {
            throw $this->createNotFoundException('User not found');
        }

        $portfolioItems = $em->getRepository(\App\Entity\PortfolioItem::class)->findBy(['user' => $targetUser]);
        
        $symbols = array_map(fn($item) => $item->getSymbol(), $portfolioItems);
        $prices = $marketService->getPrices($symbols);
        $usdToTndRate = $marketService->getUsdToTndRate();
        
        $totalValueTnd = 0.0;
        $itemsData = [];
        
        foreach ($portfolioItems as $item) {
            $symbolUpper = strtoupper($item->getSymbol());
            $priceData = $prices[$symbolUpper] ?? ['price' => 0.0, 'change_24h' => 0.0];
            
            $priceUsd = $priceData['price'];
            $quantity = (float) $item->getQuantity();
            
            $valTnd = $priceUsd * $quantity * $usdToTndRate;
            $totalValueTnd += $valTnd;
            
            // Map common symbol to icon ticker like in java getTicker()
            $ticker = strtolower($symbolUpper);
            $tickerMap = [
                'BITCOIN' => 'btc', 'ETHEREUM' => 'eth', 'BINANCECOIN' => 'bnb',
                'SOLANA' => 'sol', 'RIPPLE' => 'xrp', 'CARDANO' => 'ada',
                'AVALANCHE-2' => 'avax', 'DOGECOIN' => 'doge', 'POLKADOT' => 'dot'
            ];
            
            $iconTicker = $tickerMap[$symbolUpper] ?? substr($ticker, 0, 3);
            
            $itemsData[] = [
                'symbol' => $symbolUpper,
                'iconUrl' => "https://assets.coincap.io/assets/icons/{$iconTicker}@2x.png",
                'originalSymbol' => $item->getSymbol(),
                'quantity' => $quantity,
                'priceUsd' => $priceUsd,
                'valTnd' => round($valTnd, 2),
                'averageCost' => (float) $item->getAverageCost(),
            ];
        }
        
        return $this->render('admin/user_portfolio.html.twig', [
            'targetUser' => $targetUser,
            'items' => $itemsData,
            'totalValueTnd' => number_format($totalValueTnd, 2, '.', ''),
            'usdToTndRate' => $usdToTndRate,
        ]);
    }
}
