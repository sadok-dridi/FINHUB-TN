<?php

namespace App\Controller\Admin;

use App\Entity\User;
use App\Repository\UserRepository;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Annotation\Route;
use Symfony\Component\Security\Http\Attribute\IsGranted;

#[IsGranted('ROLE_ADMIN')]
class TransactionController extends AbstractController
{
    #[Route('/admin/transactions', name: 'app_admin_transactions')]
    public function index(Request $request, UserRepository $userRepository): Response
    {
        $sort = (string) $request->query->get('sort', 'name_asc');
        $qb = $userRepository->createQueryBuilder('u')
            ->leftJoin('u.wallet', 'w')
            ->addSelect('w');

        switch ($sort) {
            case 'name_desc':
                $qb->orderBy('u.full_name', 'DESC')->addOrderBy('u.email', 'ASC');
                break;
            case 'email_asc':
                $qb->orderBy('u.email', 'ASC');
                break;
            case 'email_desc':
                $qb->orderBy('u.email', 'DESC');
                break;
            case 'wallet_status':
                $qb
                    ->addSelect("CASE WHEN w.status = 'FROZEN' THEN 0 WHEN w.status = 'ACTIVE' THEN 1 ELSE 2 END AS HIDDEN walletSort")
                    ->orderBy('walletSort', 'ASC')
                    ->addOrderBy('u.full_name', 'ASC');
                break;
            case 'name_asc':
            default:
                $sort = 'name_asc';
                $qb->orderBy('u.full_name', 'ASC')->addOrderBy('u.email', 'ASC');
                break;
        }

        $users = $qb->getQuery()->getResult();

        return $this->render('admin/transactions.html.twig', [
            'users' => $users,
            'sort' => $sort,
        ]);
    }

    #[Route('/admin/users/{id}/transactions', name: 'app_admin_user_transactions')]
    public function userTransactions(User $targetUser, EntityManagerInterface $em): Response
    {
        if ($targetUser->getRole() === 'ADMIN' || $targetUser->getRole() === 'ROLE_ADMIN') {
            return $this->redirectToRoute('app_admin_transactions');
        }

        $wallet = $targetUser->getWallet();
        $transactions = [];
        $tamperedId = -1;
        $ledgerFlag = null;
        
        if ($wallet) {
            $transactions = $em->getRepository(\App\Entity\WalletTransaction::class)
                ->findBy(['wallet' => $wallet], ['created_at' => 'DESC']);
                
            // Check for tampered transactions
            $ascendingTx = array_reverse($transactions);
            $prevHash = str_repeat('0', 64);
            
            foreach ($ascendingTx as $tx) {
                if ($tx->getPrevHash() !== $prevHash) {
                    $tamperedId = $tx->getId();
                    break;
                }
                
                $amountStr = number_format((float)$tx->getAmount(), 3, '.', '');
                
                // Mimic Java LocalDateTime.toString() which omits seconds if they are 00
                $dt = $tx->getCreatedAt();
                $timeString = $dt->format('Y-m-d\TH:i:s');
                if (substr($timeString, -3) === ':00') {
                    $timeString = substr($timeString, 0, -3);
                }
                
                $dataToHash = $prevHash . $wallet->getId() . $tx->getType() . $amountStr . $tx->getReference() . $timeString;
                $calculatedHash = hash('sha256', $dataToHash);
                
                if ($calculatedHash !== $tx->getTxHash()) {
                    $tamperedId = $tx->getId();
                    break;
                }
                
                $prevHash = $tx->getTxHash();
            }
            
            // If frozen, get the latest flag
            if ($wallet->getStatus() === 'FROZEN') {
                $conn = $em->getConnection();
                try {
                    $stmt = $conn->prepare('SELECT reason, flagged_at FROM ledger_flags WHERE wallet_id = :wId ORDER BY flagged_at DESC LIMIT 1');
                    $res = $stmt->executeQuery(['wId' => $wallet->getId()]);
                    $ledgerFlag = $res->fetchAssociative();
                } catch (\Exception $e) {}
            }
        }

        return $this->render('admin/user_transactions.html.twig', [
            'targetUser' => $targetUser,
            'wallet' => $wallet,
            'transactions' => $transactions,
            'tamperedId' => $tamperedId,
            'ledgerFlag' => $ledgerFlag
        ]);
    }
}
