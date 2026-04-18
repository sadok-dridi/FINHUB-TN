<?php

namespace App\Controller\Admin;

use App\Entity\Escrow;
use App\Entity\User;
use App\Entity\Wallet;
use App\Entity\WalletTransaction;
use App\Repository\EscrowRepository;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Annotation\Route;
use Symfony\Component\Security\Http\Attribute\IsGranted;

#[IsGranted('ROLE_ADMIN')]
class EscrowController extends AbstractController
{
    #[Route('/admin/escrow', name: 'app_admin_escrow')]
    public function index(Request $request, EscrowRepository $escrowRepository): Response
    {
        $search = trim((string) $request->query->get('search', ''));
        $sort = (string) $request->query->get('sort', 'created');
        $direction = strtolower((string) $request->query->get('direction', 'desc')) === 'asc' ? 'ASC' : 'DESC';

        $qb = $escrowRepository->createQueryBuilder('e')
            ->leftJoin('e.senderWallet', 'sw')
            ->leftJoin('sw.user', 'su')
            ->leftJoin('e.receiverWallet', 'rw')
            ->leftJoin('rw.user', 'ru')
            ->addSelect('sw', 'su', 'rw', 'ru');

        if ($search !== '') {
            $searchTerm = '%' . mb_strtolower($search) . '%';
            $qb
                ->andWhere("LOWER(COALESCE(su.full_name, su.email, '')) LIKE :search OR LOWER(COALESCE(ru.full_name, ru.email, '')) LIKE :search OR LOWER(COALESCE(e.status, '')) LIKE :search OR LOWER(COALESCE(e.conditionText, '')) LIKE :search")
                ->setParameter('search', $searchTerm);
        }

        switch ($sort) {
            case 'amount':
                $qb->orderBy('e.amount', $direction)->addOrderBy('e.id', 'DESC');
                break;
            case 'status':
                $qb->orderBy('e.status', $direction)->addOrderBy('e.id', 'DESC');
                break;
            case 'created':
            default:
                $sort = 'created';
                $qb->orderBy('e.id', $direction);
                break;
        }

        $escrows = $qb->getQuery()->getResult();
        
        $total = count($escrows);
        $activeLocked = 0;
        $disputed = 0;
        $completed = 0;
        
        foreach ($escrows as $escrow) {
            $status = $escrow->getStatus();
            if ($status === 'LOCKED') {
                $activeLocked++;
            } elseif ($status === 'DISPUTED') {
                $disputed++;
            } elseif ($status === 'RELEASED') {
                $completed++;
            }
        }

        return $this->render('admin/escrow.html.twig', [
            'escrows' => $escrows,
            'total' => $total,
            'activeLocked' => $activeLocked,
            'disputed' => $disputed,
            'completed' => $completed,
            'search' => $search,
            'sort' => $sort,
            'direction' => strtolower($direction),
        ]);
    }

    #[Route('/admin/escrow/{id}', name: 'app_admin_escrow_details', methods: ['GET'])]
    public function details(int $id, EscrowRepository $escrowRepository): Response
    {
        $escrow = $escrowRepository->find($id);

        if (!$escrow) {
            throw $this->createNotFoundException('Escrow not found');
        }

        return $this->render('admin/escrow_details.html.twig', [
            'escrow' => $escrow,
        ]);
    }

    #[Route('/admin/escrow/{id}/release', name: 'app_admin_escrow_force_release', methods: ['POST'])]
    public function forceRelease(Escrow $escrow, EntityManagerInterface $em): JsonResponse
    {
        if (!in_array($escrow->getStatus(), ['LOCKED', 'DISPUTED'])) {
            return new JsonResponse(['success' => false, 'message' => 'Escrow is not locked or disputed.']);
        }

        $amount = (float) $escrow->getAmount();
        $fee = $amount * 0.01;
        $netAmount = $amount - $fee;

        $senderWallet = $escrow->getSenderWallet();
        $receiverWallet = $escrow->getReceiverWallet();

        // Admin (Bank) Wallet
        $adminUser = $em->getRepository(User::class)->findOneBy(['email' => 'sadok.dridi.engineer@gmail.com']);
        $adminWallet = $adminUser ? $adminUser->getWallet() : null;

        // Sender
        $senderWallet->setEscrowBalance((string) ((float) $senderWallet->getEscrowBalance() - $amount));
        $this->recordTransaction($em, $senderWallet, 'ESCROW_SENT', $amount, 'Admin Force Released to Wallet ' . $receiverWallet->getId());

        // Receiver
        $receiverWallet->setBalance((string) ((float) $receiverWallet->getBalance() + $netAmount));
        $this->recordTransaction($em, $receiverWallet, 'ESCROW_RCVD', $netAmount, 'Admin Force Received from Escrow Wallet ' . $senderWallet->getId());

        // Admin Fee
        if ($adminWallet && $fee > 0) {
            $adminWallet->setBalance((string) ((float) $adminWallet->getBalance() + $fee));
            $this->recordTransaction($em, $adminWallet, 'ESCROW_FEE', $fee, 'Fee from Escrow ' . $senderWallet->getId());
        }

        $escrow->setStatus('RELEASED');
        $em->flush();

        $this->logToBlockchain(
            $em,
            'ESCROW_RELEASE',
            "Admin Force Released Escrow " . $escrow->getId(),
            null,
            $escrow->getId()
        );

        // Increase trust score for receiver (seller)
        $receiverUser = $receiverWallet->getUser();
        $currentScore = $receiverUser->getTrustScore() ?? 100;
        $receiverUser->setTrustScore($currentScore + 10);
        $em->flush();

        return new JsonResponse(['success' => true, 'message' => 'Funds force-released successfully.']);
    }

    #[Route('/admin/escrow/{id}/refund', name: 'app_admin_escrow_force_refund', methods: ['POST'])]
    public function forceRefund(Escrow $escrow, EntityManagerInterface $em): JsonResponse
    {
        if (!in_array($escrow->getStatus(), ['LOCKED', 'DISPUTED'])) {
            return new JsonResponse(['success' => false, 'message' => 'Escrow is not locked or disputed.']);
        }

        $senderWallet = $escrow->getSenderWallet();
        $amount = (float) $escrow->getAmount();
        
        $senderWallet->setEscrowBalance((string) ((float) $senderWallet->getEscrowBalance() - $amount));
        $senderWallet->setBalance((string) ((float) $senderWallet->getBalance() + $amount));

        $this->recordTransaction($em, $senderWallet, 'ESCROW_REFUND', $amount, 'Admin Force Refunded from Escrow');

        $escrow->setStatus('REFUNDED');
        $em->flush();

        $this->logToBlockchain(
            $em,
            'ESCROW_REFUND',
            "Admin Force Refunded Escrow " . $escrow->getId(),
            null,
            $escrow->getId()
        );

        return new JsonResponse(['success' => true, 'message' => 'Funds force-refunded to sender successfully.']);
    }

    private function recordTransaction(EntityManagerInterface $em, Wallet $wallet, string $type, float $amount, string $reference): WalletTransaction
    {
        $prevTransaction = $em->getRepository(WalletTransaction::class)->findOneBy(
            ['wallet' => $wallet],
            ['created_at' => 'DESC']
        );
        $prevHash = $prevTransaction ? $prevTransaction->getTxHash() : "0000000000000000000000000000000000000000000000000000000000000000";
        
        $now = new \DateTimeImmutable('now', new \DateTimeZone('Africa/Tunis'));
        
        $timeStr = $now->format('Y-m-d\TH:i:s');
        if (substr($timeStr, -3) === ':00') {
            $timeStr = substr($timeStr, 0, -3);
        }

        $amountStr = number_format($amount, 3, '.', '');
        $dataToHash = $prevHash . $wallet->getId() . $type . $amountStr . $reference . $timeStr;
        $txHash = hash('sha256', $dataToHash);

        $walletTransaction = new WalletTransaction();
        $walletTransaction->setWallet($wallet);
        $walletTransaction->setType($type);
        $walletTransaction->setAmount($amountStr);
        $walletTransaction->setReference($reference);
        $walletTransaction->setPrevHash($prevHash);
        $walletTransaction->setTxHash($txHash);
        $walletTransaction->setCreatedAt($now);

        $em->persist($walletTransaction);
        $em->flush();
        
        $this->logToBlockchain(
            $em,
            'TRANSACTION',
            "Wallet: " . $wallet->getId() . ", Type: $type, Amount: $amountStr, Ref: $reference",
            $walletTransaction->getId(),
            null
        );

        return $walletTransaction;
    }

    private function logToBlockchain(EntityManagerInterface $em, string $type, string $data, ?int $walletTxId = null, ?int $escrowId = null): void
    {
        $conn = $em->getConnection();

        $prevHash = $conn->fetchOne("SELECT current_hash FROM blockchain_ledger ORDER BY id DESC LIMIT 1");
        if (!$prevHash) {
            $prevHash = "0000000000000000000000000000000000000000000000000000000000000000";
        }

        $dataHash = hash('sha256', $data);
        $nonce = 0;
        
        $now = new \DateTimeImmutable('now', new \DateTimeZone('Africa/Tunis'));
        $dbTimestamp = $now->format('Y-m-d H:i:s');
        $timeStr = $dbTimestamp . '.0';

        $input = $prevHash . $dataHash . $type . $nonce . $timeStr;
        $currentHash = hash('sha256', $input);

        $sql = "INSERT INTO blockchain_ledger (previous_hash, data_hash, type, nonce, timestamp, current_hash, wallet_transaction_id, escrow_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        $conn->executeStatement($sql, [
            $prevHash, $dataHash, $type, $nonce, $dbTimestamp, $currentHash, $walletTxId, $escrowId
        ]);
    }
}
