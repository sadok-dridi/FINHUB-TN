<?php

namespace App\Controller\User;

use App\Entity\User;
use App\Entity\PortfolioItem;
use App\Service\MarketService;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Annotation\Route;
use Symfony\Component\Security\Http\Attribute\IsGranted;
use App\Form\TransferType;
use App\Form\CashInType;
use App\Form\CardCashInType;

#[IsGranted('ROLE_USER')]
class DashboardController extends AbstractController
{
    #[Route('/dashboard', name: 'app_user_dashboard')]
    public function index(Request $request, EntityManagerInterface $entityManager, MarketService $marketService, \App\Service\WalletService $walletService): Response
    {
        /** @var User $user */
        $user = $this->getUser();
        $wallet = $user->getWallet();

        // Safe defaults if wallet doesn't exist locally yet
        $balance = $wallet ? $wallet->getBalance() : '0.00';
        $escrowBalance = $wallet ? $wallet->getEscrowBalance() : '0.00';
        $currency = $wallet ? $wallet->getCurrency() : 'TND';
        $walletStatus = $wallet ? $wallet->getStatus() : 'NO WALLET';
        
        $rawTransactions = $wallet ? $wallet->getTransactions()->slice(0, 5) : [];
        $recentTransactions = [];

        foreach ($rawTransactions as $tx) {
            $ref = $tx->getReference() ?? '';
            $counterpartyName = 'System';
            $avatarUrl = null;

            // Extract the name from strings like "Transfer to/from John Doe (Wallet 123)"
            // or "Escrow Released to Jane"
            if (preg_match('/(?:to|from) (.+?)(?: \((?:Wallet|User)|$)/i', $ref, $matches)) {
                $counterpartyName = trim($matches[1]);
                
                // Try to find the user in the database to get their actual profile photo
                $counterpartyUser = $entityManager->getRepository(User::class)->findOneBy(['full_name' => $counterpartyName]);
                if ($counterpartyUser && $counterpartyUser->getProfilePhotoUrl()) {
                    $avatarUrl = $counterpartyUser->getProfilePhotoUrl();
                }
            }

            // Fallback to generated avatar if no photo exists
            if (!$avatarUrl) {
                // Use a default seed for system transactions, or the person's name
                $seed = $counterpartyName === 'System' ? 'finhub' : urlencode($counterpartyName);
                $avatarUrl = 'https://api.dicebear.com/7.x/avataaars/svg?seed=' . $seed;
            }

            // Determine if the transaction is an outflow (negative balance impact)
            $isOutflow = in_array(strtoupper($tx->getType()), [
                'TRANSFER_SENT', 
                'DEBIT', 
                'HOLD', 
                'ESCROW_SENT', 
                'ESCROW_FEE',
                'WITHDRAWAL'
            ]);

            $isTampered = false;
            if ($walletStatus === 'FROZEN') {
                $isTampered = $walletService->isTransactionTampered($tx);
            }

            $recentTransactions[] = [
                'tx' => $tx,
                'displayName' => $counterpartyName,
                'avatarUrl' => $avatarUrl,
                'isOutflow' => $isOutflow,
                'isTampered' => $isTampered,
            ];
        }

        $virtualCardInfo = null;
        if ($wallet && count($wallet->getVirtualCards()) > 0) {
            $card = $wallet->getVirtualCards()->first();
            
            // Format card number: split into chunks of 4
            $formattedCardNumber = trim(chunk_split($card->getCardNumber(), 4, ' '));
            
            // Format expiry date: from 'YYYY-MM-DD' to 'YYYY-MM'
            $formattedExpiry = '';
            if ($card->getExpiryDate()) {
                $formattedExpiry = $card->getExpiryDate()->format('Y-m');
            }

            $virtualCardInfo = [
                'cardNumber' => $formattedCardNumber,
                'cvv' => $card->getCvv(),
                'expiryDate' => $formattedExpiry,
            ];
        }

        // --- Market Portfolio Logic ---
        $portfolioItems = $entityManager->getRepository(PortfolioItem::class)->findBy(['user' => $user]);
        $symbols = array_map(fn($item) => $item->getSymbol(), $portfolioItems);
        
        $marketPrices = $marketService->getPrices($symbols);
        $usdToTndRate = $marketService->getUsdToTndRate();

        $totalValueTnd = 0.0;
        $totalCostTnd = 0.0;
        $topAsset = null;
        $topAssetPerformance = -9999.0;
        $numAssets = count($portfolioItems);

        foreach ($portfolioItems as $item) {
            $sym = strtoupper($item->getSymbol());
            $qty = (float) $item->getQuantity();
            $avgCostUsd = (float) $item->getAverageCost();

            $costTnd = $qty * $avgCostUsd * $usdToTndRate;
            $totalCostTnd += $costTnd;

            if (isset($marketPrices[$sym])) {
                $currentUsd = $marketPrices[$sym]['price'];
                $change24h = $marketPrices[$sym]['change_24h'];
                
                $valTnd = $qty * $currentUsd * $usdToTndRate;
                $totalValueTnd += $valTnd;

                if ($change24h > $topAssetPerformance) {
                    $topAssetPerformance = $change24h;
                    $topAsset = $sym;
                }
            } else {
                // Fallback if API doesn't have price: assume cost = value
                $totalValueTnd += $costTnd;
            }
        }

        $calcDiff = $totalValueTnd - $totalCostTnd;
        $calcChangePct = $totalCostTnd > 0 ? ($calcDiff / $totalCostTnd) * 100 : 0.0;

        $portfolioStats = [
            'totalAssetValue' => $totalValueTnd,
            'costBasis' => $totalCostTnd,
            'profitLoss' => $calcDiff,
            'changePercent' => $calcChangePct,
            'numAssets' => $numAssets,
            'topAsset' => $topAsset ?? 'N/A',
            'topAssetPerformance' => $topAssetPerformance !== -9999.0 ? $topAssetPerformance : 0.0,
        ];

        $transferForm = $this->createForm(TransferType::class);
        $cashInForm = $this->createForm(CashInType::class);
        $cardCashInForm = $this->createForm(CardCashInType::class);

        return $this->render('user/dashboard.html.twig', [
            'user' => $user,
            'balance' => $balance,
            'escrowBalance' => $escrowBalance,
            'currency' => $currency,
            'walletStatus' => $walletStatus,
            'recentTransactions' => $recentTransactions,
            'virtualCard' => $virtualCardInfo,
            'portfolioStats' => $portfolioStats,
            'transferForm' => $transferForm->createView(),
            'cashInForm' => $cashInForm->createView(),
            'cardCashInForm' => $cardCashInForm->createView(),
        ]);
    }
}
