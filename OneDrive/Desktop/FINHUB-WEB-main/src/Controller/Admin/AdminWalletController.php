<?php

namespace App\Controller\Admin;

use App\Entity\VirtualCard;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Annotation\Route;
use Symfony\Component\Security\Http\Attribute\IsGranted;

#[IsGranted('ROLE_ADMIN')]
class AdminWalletController extends AbstractController
{
    #[Route('/admin/wallets', name: 'app_admin_wallets')]
    public function index(EntityManagerInterface $em): Response
    {
        $cards = $em->createQueryBuilder()
            ->select('c')
            ->from(VirtualCard::class, 'c')
            ->join('c.wallet', 'w')
            ->join('w.user', 'u')
            ->getQuery()
            ->getResult();

        // Calculate Flow & Adoption Stats
        $thirtyDaysAgo = new \DateTimeImmutable('-30 days');
        $transactions = $em->getRepository(\App\Entity\WalletTransaction::class)
            ->createQueryBuilder('t')
            ->where('t.created_at >= :date')
            ->setParameter('date', $thirtyDaysAgo)
            ->getQuery()
            ->getResult();

        $deposits30d = 0;
        $transfers30d = 0;
        $dailyVolume = array_fill(0, 7, 0);
        $labels = [];

        $today = new \DateTimeImmutable('today');
        for ($i = 6; $i >= 0; $i--) {
            $labels[] = $today->modify("-$i days")->format('M d');
        }

        foreach ($transactions as $tx) {
            $amount = abs((float)$tx->getAmount());
            $type = $tx->getType();
            
            if (in_array($type, ['DEPOSIT', 'CASH_IN'])) {
                $deposits30d += $amount;
            } elseif (in_array($type, ['TRANSFER_SENT', 'TRANSFER'])) {
                // Only count the send leg to avoid double counting internal transfers
                $transfers30d += $amount;
            }

            $txDate = $tx->getCreatedAt()->setTime(0, 0);
            $diffDays = $today->diff($txDate)->days;
            $invert = $today->diff($txDate)->invert; // 1 if past
            if ($invert && $diffDays < 7) {
                // diffDays goes from 0 (today) to 6. Index should be 6 (today) down to 0
                $dailyVolume[6 - $diffDays] += $amount;
            } elseif (!$invert && $diffDays === 0) {
                $dailyVolume[6] += $amount; // Today
            }
        }
        
        // Calculate Card Status Distribution
        $activeCards = 0; $frozenCards = 0; $inactiveCards = 0;
        foreach ($cards as $card) {
            $status = strtoupper($card->getStatus());
            if ($status === 'ACTIVE') $activeCards++;
            elseif ($status === 'FROZEN') $frozenCards++;
            else $inactiveCards++;
        }

        return $this->render('admin/wallets.html.twig', [
            'cards' => $cards,
            'stats' => [
                'deposits30d' => $deposits30d,
                'transfers30d' => $transfers30d,
                'dailyVolume' => $dailyVolume,
                'chartLabels' => $labels,
                'activeCards' => $activeCards,
                'frozenCards' => $frozenCards,
                'inactiveCards' => $inactiveCards
            ]
        ]);
    }

    #[Route('/admin/wallets/card/{id}/edit', name: 'app_admin_wallet_card_edit', methods: ['POST'])]
    public function editCard(int $id, \Symfony\Component\HttpFoundation\Request $request, EntityManagerInterface $em): \Symfony\Component\HttpFoundation\JsonResponse
    {
        $card = $em->getRepository(VirtualCard::class)->find($id);
        if (!$card) {
            return new \Symfony\Component\HttpFoundation\JsonResponse(['success' => false, 'message' => 'Card not found'], 404);
        }

        $data = json_decode($request->getContent(), true);

        if (isset($data['expiryDate']) && !empty($data['expiryDate'])) {
            try {
                // Expected format: YYYY-MM
                $dateString = $data['expiryDate'] . '-01'; // Add 1st day to make it a full date easily parsed
                $newExpiry = new \DateTime($dateString);
                // Set to the end of the month
                $newExpiry->modify('last day of this month')->setTime(23, 59, 59);
                $card->setExpiryDate($newExpiry);
            } catch (\Exception $e) {
                return new \Symfony\Component\HttpFoundation\JsonResponse(['success' => false, 'message' => 'Invalid date format']);
            }
        }

        if (isset($data['status']) && in_array($data['status'], ['ACTIVE', 'INACTIVE', 'FROZEN'])) {
            $card->setStatus($data['status']);
        }

        $em->flush();

        $this->addFlash('success', 'Virtual card updated successfully.');
        return new \Symfony\Component\HttpFoundation\JsonResponse(['success' => true]);
    }
}
