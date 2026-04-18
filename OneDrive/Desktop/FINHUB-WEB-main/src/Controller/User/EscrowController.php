<?php

namespace App\Controller\User;

use App\Entity\TrustedContact;
use App\Entity\User;
use App\Entity\Escrow;
use App\Entity\WalletTransaction;
use App\Service\DocuSignService;
use Symfony\Contracts\HttpClient\HttpClientInterface;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Annotation\Route;
use Symfony\Component\Security\Http\Attribute\IsGranted;

use App\Form\EscrowType;
use Symfony\Component\Form\FormFactoryInterface;

#[IsGranted('ROLE_USER')]
class EscrowController extends AbstractController
{
    #[Route('/user/escrow', name: 'app_user_escrow')]
    public function index(Request $request, EntityManagerInterface $em): Response
    {
        /** @var User $user */
        $user = $this->getUser();
        $trustScore = $user->getTrustScore() ?? 100; // Default if null

        // Fetch Trusted Contacts for Escrow
        $trustedContactsRaw = $em->getRepository(TrustedContact::class)->findBy(['user' => $user]);
        $contacts = [];

        foreach ($trustedContactsRaw as $tc) {
            $contactUser = $tc->getContactUser();
            if (!$contactUser) {
                continue;
            }

            $displayName = $tc->getAlias() ?: ($contactUser->getFullName() ?: $contactUser->getEmail());
            $email = $contactUser->getEmail();
            $phone = $tc->getPhone() ?: ($contactUser->getPhoneNumber() ?: 'N/A');
            $contactTrustScore = $contactUser->getTrustScore() ?? 100;
            $avatarUrl = $contactUser->getProfilePhotoUrl();
            $initial = strtoupper(substr(trim($displayName), 0, 2));

            // Fetch a few recent escrows with this contact
            $walletId = $user->getWallet() ? $user->getWallet()->getId() : null;
            $contactWalletId = $contactUser->getWallet() ? $contactUser->getWallet()->getId() : null;
            
            $history = [];
            if ($walletId && $contactWalletId) {
                // Find escrows where (sender=user AND receiver=contact) OR (sender=contact AND receiver=user)
                $escrows = $em->createQueryBuilder()
                    ->select('e')
                    ->from(Escrow::class, 'e')
                    ->where('(e.senderWallet = :w1 AND e.receiverWallet = :w2) OR (e.senderWallet = :w2 AND e.receiverWallet = :w1)')
                    ->setParameter('w1', $walletId)
                    ->setParameter('w2', $contactWalletId)
                    ->orderBy('e.createdAt', 'DESC')
                    ->setMaxResults(5)
                    ->getQuery()
                    ->getResult();

                foreach ($escrows as $escrow) {
                    $history[] = [
                        'id' => $escrow->getId(),
                        'amount' => $escrow->getAmount(),
                        'status' => $escrow->getStatus(),
                        'date' => $escrow->getCreatedAt() ? $escrow->getCreatedAt()->format('M d, Y') : 'Unknown',
                    ];
                }
            }

            $contacts[] = [
                'id' => $tc->getId(),
                'contactUserId' => $contactUser->getId(),
                'displayName' => $displayName,
                'email' => $email,
                'phone' => $phone,
                'trustScore' => $contactTrustScore,
                'avatarUrl' => $avatarUrl,
                'initial' => $initial,
                'walletStatus' => $contactUser->getWallet() ? $contactUser->getWallet()->getStatus() : 'NO WALLET',
                'history' => $history
            ];
        }

        // Fetch All Escrows
        $allEscrowsRaw = [];
        if ($user->getWallet()) {
            $allEscrowsRaw = $em->getRepository(Escrow::class)->createQueryBuilder('e')
                ->where('e.senderWallet = :w OR e.receiverWallet = :w')
                ->setParameter('w', $user->getWallet())
                ->orderBy('e.createdAt', 'DESC')
                ->getQuery()
                ->getResult();
        }

        $activeEscrows = [];
        $historyEscrows = [];
        $disputedEscrows = [];

        foreach ($allEscrowsRaw as $e) {
            $isSender = $e->getSenderWallet() === $user->getWallet();
            $otherParty = $isSender ? $e->getReceiverWallet()->getUser() : $e->getSenderWallet()->getUser();
            
            $escrowData = [
                'id' => $e->getId(),
                'amount' => number_format((float)$e->getAmount(), 3, '.', ''),
                'status' => $e->getStatus(),
                'condition' => $e->getConditionText(),
                'isSender' => $isSender,
                'otherPartyName' => $otherParty->getFullName() ?: $otherParty->getEmail(),
                'secretCode' => $e->getSecretCode(),
                'qrCodeImage' => $e->getQrCodeImage(),
                'requireDocusign' => $e->isRequireDocusign(),
                'docusignEnvelopeId' => $e->getDocusignEnvelopeId(),
                'type' => $e->getEscrowType(),
                'isExpired' => $e->getExpiryDate() < new \DateTime(),
            ];

            if ($e->getStatus() === 'LOCKED') {
                $activeEscrows[] = $escrowData;
            } elseif ($e->getStatus() === 'DISPUTED') {
                $disputedEscrows[] = $escrowData;
            } else {
                $historyEscrows[] = $escrowData;
            }
        }

        $activeTab = (string) $request->query->get('tab', 'contacts');
        if (!in_array($activeTab, ['contacts', 'active', 'history', 'disputes'], true)) {
            $activeTab = 'contacts';
        }

        $search = trim((string) $request->query->get('search', ''));
        $sort = (string) $request->query->get('sort', $activeTab === 'contacts' ? 'name' : 'created');
        $direction = strtolower((string) $request->query->get('direction', 'desc')) === 'asc' ? 'asc' : 'desc';

        $sortByDirection = static function (int $comparison) use ($direction): int {
            return $direction === 'asc' ? $comparison : -$comparison;
        };

        if ($activeTab === 'contacts') {
            if ($search !== '') {
                $needle = mb_strtolower($search);
                $contacts = array_values(array_filter($contacts, static function (array $contact) use ($needle) {
                    $haystack = implode(' ', [
                        (string) $contact['displayName'],
                        (string) $contact['email'],
                        (string) $contact['phone'],
                        (string) $contact['trustScore'],
                    ]);

                    return str_contains(mb_strtolower($haystack), $needle);
                }));
            }

            usort($contacts, static function (array $a, array $b) use ($sort, $sortByDirection) {
                $comparison = match ($sort) {
                    'trust' => $a['trustScore'] <=> $b['trustScore'],
                    'email' => strcasecmp((string) $a['email'], (string) $b['email']),
                    default => strcasecmp((string) $a['displayName'], (string) $b['displayName']),
                };

                return $sortByDirection($comparison);
            });
        } else {
            $targetList = match ($activeTab) {
                'active' => $activeEscrows,
                'history' => $historyEscrows,
                'disputes' => $disputedEscrows,
                default => [],
            };

            if ($search !== '') {
                $needle = mb_strtolower($search);
                $targetList = array_values(array_filter($targetList, static function (array $escrow) use ($needle) {
                    $haystack = implode(' ', [
                        (string) $escrow['id'],
                        (string) $escrow['amount'],
                        (string) $escrow['status'],
                        (string) $escrow['condition'],
                        (string) $escrow['otherPartyName'],
                        (string) $escrow['type'],
                    ]);

                    return str_contains(mb_strtolower($haystack), $needle);
                }));
            }

            usort($targetList, static function (array $a, array $b) use ($sort, $sortByDirection) {
                $comparison = match ($sort) {
                    'amount' => (float) $a['amount'] <=> (float) $b['amount'],
                    'status' => strcasecmp((string) $a['status'], (string) $b['status']),
                    'party' => strcasecmp((string) $a['otherPartyName'], (string) $b['otherPartyName']),
                    default => $a['id'] <=> $b['id'],
                };

                return $sortByDirection($comparison);
            });

            if ($activeTab === 'active') {
                $activeEscrows = $targetList;
            } elseif ($activeTab === 'history') {
                $historyEscrows = $targetList;
            } elseif ($activeTab === 'disputes') {
                $disputedEscrows = $targetList;
            }
        }

        $escrowForm = $this->createForm(EscrowType::class);

        return $this->render('user/escrow.html.twig', [
            'trustScore' => $trustScore,
            'contacts' => $contacts,
            'activeEscrows' => $activeEscrows,
            'historyEscrows' => $historyEscrows,
            'disputedEscrows' => $disputedEscrows,
            'activeTab' => $activeTab,
            'search' => $search,
            'sort' => $sort,
            'direction' => $direction,
            'escrowForm' => $escrowForm->createView(),
        ]);
    }

    #[Route('/user/escrow/contact/add', name: 'app_user_escrow_contact_add', methods: ['POST'])]
    public function addContact(Request $request, EntityManagerInterface $em, FormFactoryInterface $formFactory): JsonResponse
    {
        /** @var User $user */
        $user = $this->getUser();
        
        $data = json_decode($request->getContent(), true);

        $form = $formFactory->create(\App\Form\ContactType::class);
        $form->submit($data);

        $errors = [];

        if (!$form->isValid()) {
            foreach ($form->getErrors(true) as $error) {
                $fieldName = $error->getOrigin()->getName();
                $errors[$fieldName] = $error->getMessage();
            }
            return new JsonResponse(['success' => false, 'errors' => $errors]);
        }

        $email = $form->get('email')->getData();

        if (strtolower($email) === strtolower($user->getEmail())) {
            return new JsonResponse(['success' => false, 'errors' => ['email' => 'You cannot save yourself as a contact.']]);
        }

        $contactUser = $em->getRepository(User::class)->findOneBy(['email' => $email]);

        if (!$contactUser) {
            return new JsonResponse(['success' => false, 'errors' => ['email' => 'No user found with this email in FinHub.']]);
        }

        if (!$contactUser->getWallet()) {
            return new JsonResponse(['success' => false, 'errors' => ['email' => 'User exists but has no active wallet.']]);
        }

        $existingContact = $em->getRepository(TrustedContact::class)->findOneBy([
            'user' => $user,
            'contactUser' => $contactUser
        ]);

        if ($existingContact) {
            return new JsonResponse(['success' => false, 'errors' => ['email' => 'Contact with this email already exists.']]);
        }

        $contact = new TrustedContact();
        $contact->setUser($user);
        $contact->setContactUser($contactUser);
        
        // Use the contact's full name as default alias
        $alias = $contactUser->getFullName() ?: $contactUser->getEmail();
        $contact->setAlias($alias);

        $em->persist($contact);
        $em->flush();

        return new JsonResponse(['success' => true, 'message' => 'Contact successfully added.']);
    }

    #[Route('/user/escrow/contact/{id}/remove', name: 'app_user_escrow_contact_remove', methods: ['POST'])]
    public function removeContact(int $id, EntityManagerInterface $em): JsonResponse
    {
        /** @var User $user */
        $user = $this->getUser();
        
        // Find by user and contactUser (since 'id' might be problematic with the legacy Java schema)
        $contactUser = $em->getRepository(User::class)->find($id);
        
        if (!$contactUser) {
            return new JsonResponse(['success' => false, 'message' => 'Contact user not found.']);
        }

        $contact = $em->getRepository(TrustedContact::class)->findOneBy([
            'user' => $user,
            'contactUser' => $contactUser
        ]);

        if (!$contact) {
            return new JsonResponse(['success' => false, 'message' => 'Trusted contact not found.']);
        }

        // Use query builder to delete to avoid Doctrine ID issues if schema lacks PK
        $em->createQueryBuilder()
            ->delete(TrustedContact::class, 'tc')
            ->where('tc.user = :user')
            ->andWhere('tc.contactUser = :contact')
            ->setParameter('user', $user)
            ->setParameter('contact', $contactUser)
            ->getQuery()
            ->execute();

        return new JsonResponse(['success' => true]);
    }

    #[Route('/user/escrow/contact/{id}/alias', name: 'app_user_escrow_contact_alias', methods: ['POST'])]
    public function updateContactAlias(int $id, Request $request, EntityManagerInterface $em): JsonResponse
    {
        /** @var User $user */
        $user = $this->getUser();

        $contactUser = $em->getRepository(User::class)->find($id);
        if (!$contactUser) {
            return new JsonResponse(['success' => false, 'message' => 'Contact user not found.']);
        }

        $contact = $em->getRepository(TrustedContact::class)->findOneBy([
            'user' => $user,
            'contactUser' => $contactUser,
        ]);

        if (!$contact) {
            return new JsonResponse(['success' => false, 'message' => 'Trusted contact not found.']);
        }

        $data = json_decode($request->getContent(), true) ?? [];
        $alias = trim((string) ($data['alias'] ?? ''));

        if ($alias === '') {
            return new JsonResponse(['success' => false, 'message' => 'Alias cannot be empty.']);
        }

        if (mb_strlen($alias) > 255) {
            return new JsonResponse(['success' => false, 'message' => 'Alias must be 255 characters or less.']);
        }

        $contact->setAlias($alias);
        $em->flush();

        return new JsonResponse(['success' => true, 'alias' => $alias]);
    }

    #[Route('/user/escrow/contact/{id}/phone', name: 'app_user_escrow_contact_phone', methods: ['POST'])]
    public function updateContactPhone(int $id, Request $request, EntityManagerInterface $em): JsonResponse
    {
        /** @var User $user */
        $user = $this->getUser();

        $contactUser = $em->getRepository(User::class)->find($id);
        if (!$contactUser) {
            return new JsonResponse(['success' => false, 'message' => 'Contact user not found.']);
        }

        $contact = $em->getRepository(TrustedContact::class)->findOneBy([
            'user' => $user,
            'contactUser' => $contactUser,
        ]);

        if (!$contact) {
            return new JsonResponse(['success' => false, 'message' => 'Trusted contact not found.']);
        }

        $data = json_decode($request->getContent(), true) ?? [];
        $phone = trim((string) ($data['phone'] ?? ''));

        if ($phone === '') {
            $contact->setPhone(null);
            $em->flush();

            return new JsonResponse(['success' => true, 'phone' => $contactUser->getPhoneNumber() ?: 'N/A']);
        }

        if (mb_strlen($phone) > 20) {
            return new JsonResponse(['success' => false, 'message' => 'Phone number must be 20 characters or less.']);
        }

        if (!preg_match('/^\+?[0-9 ]{8,20}$/', $phone)) {
            return new JsonResponse(['success' => false, 'message' => 'Phone number can only contain digits, spaces, and an optional leading +.']);
        }

        $contact->setPhone($phone);
        $em->flush();

        return new JsonResponse(['success' => true, 'phone' => $phone]);
    }

    #[Route('/user/escrow/create', name: 'app_user_escrow_create', methods: ['POST'])]
    public function createEscrow(Request $request, EntityManagerInterface $em, HttpClientInterface $httpClient, DocuSignService $docuSignService, FormFactoryInterface $formFactory): JsonResponse
    {
        /** @var User $sender */
        $sender = $this->getUser();

        $data = json_decode($request->getContent(), true);

        $form = $formFactory->create(EscrowType::class);
        $form->submit($data);

        $errors = [];

        if (!$form->isValid()) {
            foreach ($form->getErrors(true) as $error) {
                $fieldName = $error->getOrigin()->getName();
                if (in_array($fieldName, ['email', 'amount', 'conditions'])) {
                    $errors[$fieldName] = $error->getMessage();
                }
            }
        }

        $email = $form->get('email')->getData() ?? '';
        $amount = (float) ($form->get('amount')->getData() ?? 0);
        $conditions = $form->get('conditions')->getData() ?? '';
        
        $type = $data['type'] ?? 'QR_CODE';
        $requireDocusign = $data['requireDocusign'] ?? false;

        $senderWallet = $sender->getWallet();

        if (empty($errors)) {
            if (strtolower($email) === strtolower($sender->getEmail())) {
                $errors['email'] = 'You cannot create an escrow with yourself.';
            } else {
                $receiver = $em->getRepository(User::class)->findOneBy(['email' => $email]);
                if (!$receiver) {
                    $errors['email'] = "User with email '$email' not found.";
                } else {
                    $receiverWallet = $receiver->getWallet();
                    if (!$receiverWallet) {
                        $errors['email'] = 'Receiver does not have an active wallet.';
                    }
                }
            }

            if (!$senderWallet) {
                return new JsonResponse(['success' => false, 'message' => 'You do not have an active wallet.']); // General error not tied to a specific field
            }

            if ($amount > 0 && $senderWallet) {
                if ((float) $senderWallet->getBalance() < $amount) {
                    $errors['amount'] = 'Insufficient funds in your wallet.';
                }
            }
        }

        if (count($errors) > 0) {
            return new JsonResponse(['success' => false, 'errors' => $errors]);
        }

        // Create Escrow
        $escrow = new Escrow();
        $escrow->setSenderWallet($senderWallet);
        $escrow->setReceiverWallet($receiverWallet);
        $escrow->setAmount((string) $amount);
        $escrow->setConditionText($conditions);
        $escrow->setEscrowType($type);
        $escrow->setRequireDocusign($requireDocusign);
        $escrow->setStatus('LOCKED');
        $escrow->setExpiryDate((new \DateTime())->modify('+7 days'));
        
        if ($type === 'QR_CODE') {
            // Basic random string for Secret Code
            $secretCode = strtoupper(substr(bin2hex(random_bytes(5)), 0, 10));
            $escrow->setSecretCode($secretCode);

            try {
                $qrUrl = 'https://api.qrserver.com/v1/create-qr-code/?size=300x300&data=' . urlencode($secretCode);
                $qrResponse = $httpClient->request('GET', $qrUrl);
                if ($qrResponse->getStatusCode() === 200) {
                    $qrContent = $qrResponse->getContent();
                    $escrow->setQrCodeImage('data:image/png;base64,' . base64_encode($qrContent));
                }
            } catch (\Exception $e) {
                // If API fails, QR code will be null (or handled later)
            }
        }

        // Docusign API Integration
        if ($requireDocusign) {
            try {
                $envelopeId = $docuSignService->createEscrowAgreementEnvelope($escrow, $sender, $receiver);
                $escrow->setDocusignEnvelopeId($envelopeId);
            } catch (\Exception $e) {
                return new JsonResponse(['success' => false, 'message' => 'DocuSign Integration Failed: ' . $e->getMessage()]);
            }
        }

        // Hold funds (subtract from balance, add to escrowBalance)
        $senderWallet->setBalance((string) ((float) $senderWallet->getBalance() - $amount));
        $senderWallet->setEscrowBalance((string) ((float) $senderWallet->getEscrowBalance() + $amount));

        $em->persist($escrow);
        $em->persist($senderWallet);
        $em->flush();

        // Create the HOLD transaction to exactly replicate Java behavior
        $conditionSummary = strlen($conditions) > 30 ? substr($conditions, 0, 30) . "..." : $conditions;
        $reference = "Escrow: " . $conditionSummary;
        
        $this->recordTransaction($em, $senderWallet, 'HOLD', $amount, $reference);

        // 2. Log Escrow Create to Blockchain
        $this->logToBlockchain(
            $em,
            'ESCROW_CREATE',
            "Created Escrow " . $escrow->getId() . " Amount: " . number_format($amount, 3, '.', ''),
            null,
            $escrow->getId()
        );

        return new JsonResponse([
            'success' => true,
            'message' => $requireDocusign 
                ? 'Escrow created with DocuSign requirement! Please check your email to sign the agreement.' 
                : 'Escrow successfully created.'
        ]);
    }

    private function recordTransaction(EntityManagerInterface $em, \App\Entity\Wallet $wallet, string $type, float $amount, string $reference): \App\Entity\WalletTransaction
    {
        $prevTransaction = $em->getRepository(\App\Entity\WalletTransaction::class)->findOneBy(
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

        $walletTransaction = new \App\Entity\WalletTransaction();
        $walletTransaction->setWallet($wallet);
        $walletTransaction->setType($type);
        $walletTransaction->setAmount($amountStr);
        $walletTransaction->setReference($reference);
        $walletTransaction->setPrevHash($prevHash);
        $walletTransaction->setTxHash($txHash);
        $walletTransaction->setCreatedAt($now);

        $em->persist($walletTransaction);
        $em->flush(); // To guarantee sequential IDs/Hashes if multiple txs happen in same flow
        
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

    #[Route('/user/escrow/{id}/release', name: 'app_user_escrow_release', methods: ['POST'])]
    public function releaseEscrow(Escrow $escrow, EntityManagerInterface $em): JsonResponse
    {
        /** @var User $user */
        $user = $this->getUser();
        $senderWallet = $escrow->getSenderWallet();

        if ($senderWallet->getUser() !== $user) {
            return new JsonResponse(['success' => false, 'message' => 'Only the sender can manually release the escrow.']);
        }

        if ($escrow->getStatus() !== 'LOCKED') {
            return new JsonResponse(['success' => false, 'message' => 'Escrow is not locked.']);
        }

        $this->processRelease($escrow, $em);

        return new JsonResponse(['success' => true, 'message' => 'Funds released successfully.']);
    }

    #[Route('/user/escrow/{id}/claim', name: 'app_user_escrow_claim', methods: ['POST'])]
    public function claimEscrow(Request $request, Escrow $escrow, EntityManagerInterface $em): JsonResponse
    {
        /** @var User $user */
        $user = $this->getUser();
        $receiverWallet = $escrow->getReceiverWallet();

        if ($receiverWallet->getUser() !== $user) {
            return new JsonResponse(['success' => false, 'message' => 'Only the receiver can claim the escrow.']);
        }

        if ($escrow->getStatus() !== 'LOCKED') {
            return new JsonResponse(['success' => false, 'message' => 'Escrow is not locked.']);
        }

        $data = json_decode($request->getContent(), true);
        $inputCode = $data['code'] ?? '';

        if ($escrow->getEscrowType() === 'QR_CODE') {
            if ($escrow->getSecretCode() !== $inputCode) {
                return new JsonResponse(['success' => false, 'message' => 'Invalid Secret Code.']);
            }
        }

        $this->processRelease($escrow, $em);

        return new JsonResponse(['success' => true, 'message' => 'Funds claimed successfully.']);
    }

    #[Route('/user/escrow/{id}/refund', name: 'app_user_escrow_refund', methods: ['POST'])]
    public function claimRefund(Escrow $escrow, EntityManagerInterface $em): JsonResponse
    {
        /** @var User $user */
        $user = $this->getUser();
        $senderWallet = $escrow->getSenderWallet();

        if ($senderWallet->getUser() !== $user) {
            return new JsonResponse(['success' => false, 'message' => 'Only the sender can claim a refund.']);
        }

        if ($escrow->getStatus() !== 'LOCKED') {
            return new JsonResponse(['success' => false, 'message' => 'Escrow is not locked.']);
        }

        $now = new \DateTime();
        if ($escrow->getExpiryDate() > $now) {
            return new JsonResponse(['success' => false, 'message' => 'Escrow has not expired yet.']);
        }

        // Process refund
        $amount = (float) $escrow->getAmount();
        $senderWallet->setEscrowBalance((string) ((float) $senderWallet->getEscrowBalance() - $amount));
        $senderWallet->setBalance((string) ((float) $senderWallet->getBalance() + $amount));

        $this->recordTransaction($em, $senderWallet, 'ESCROW_REFUND', $amount, 'Refunded from Escrow');

        $escrow->setStatus('REFUNDED');
        $em->flush();

        $this->logToBlockchain(
            $em,
            'ESCROW_REFUND',
            "Refunded Escrow " . $escrow->getId(),
            null,
            $escrow->getId()
        );

        return new JsonResponse(['success' => true, 'message' => 'Escrow expired. Funds refunded to your wallet.']);
    }

    #[Route('/user/escrow/{id}/dispute', name: 'app_user_escrow_dispute', methods: ['POST'])]
    public function raiseDispute(Escrow $escrow, EntityManagerInterface $em): JsonResponse
    {
        if ($escrow->getStatus() !== 'LOCKED') {
            return new JsonResponse(['success' => false, 'message' => 'Cannot dispute non-locked escrow.']);
        }

        $escrow->setIsDisputed(true);
        $escrow->setStatus('DISPUTED');
        $em->flush();

        $this->logToBlockchain(
            $em,
            'ESCROW_DISPUTE',
            "Dispute raised for Escrow " . $escrow->getId(),
            null,
            $escrow->getId()
        );

        return new JsonResponse(['success' => true, 'message' => 'Dispute raised. Support will contact you.']);
    }

    #[Route('/user/escrow/{id}/status', name: 'app_user_escrow_status', methods: ['GET'])]
    public function checkStatus(Escrow $escrow): JsonResponse
    {
        return new JsonResponse(['success' => true, 'status' => $escrow->getStatus()]);
    }

    #[Route('/user/escrow/{id}/delete', name: 'app_user_escrow_delete', methods: ['DELETE'])]
    public function deleteEscrow(Escrow $escrow, EntityManagerInterface $em): JsonResponse
    {
        /** @var User $user */
        $user = $this->getUser();

        $isParticipant = $escrow->getSenderWallet()?->getUser() === $user || $escrow->getReceiverWallet()?->getUser() === $user;
        if (!$isParticipant) {
            return new JsonResponse(['success' => false, 'message' => 'You are not allowed to delete this escrow.'], 403);
        }

        if (!in_array($escrow->getStatus(), ['RELEASED', 'REFUNDED'], true)) {
            return new JsonResponse(['success' => false, 'message' => 'Only released or refunded escrows can be deleted.'], 400);
        }

        $conn = $em->getConnection();

        try {
            $conn->executeStatement('DELETE FROM blockchain_ledger WHERE escrow_id = ?', [$escrow->getId()]);
        } catch (\Throwable $e) {
        }

        $em->remove($escrow);
        $em->flush();

        return new JsonResponse(['success' => true, 'message' => 'Escrow deleted successfully.']);
    }

    private function processRelease(Escrow $escrow, EntityManagerInterface $em): void
    {
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
        $this->recordTransaction($em, $senderWallet, 'ESCROW_SENT', $amount, 'Released to Wallet ' . $receiverWallet->getId());

        // Receiver
        $receiverWallet->setBalance((string) ((float) $receiverWallet->getBalance() + $netAmount));
        $this->recordTransaction($em, $receiverWallet, 'ESCROW_RCVD', $netAmount, 'Received from Escrow Wallet ' . $senderWallet->getId());

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
            "Released Escrow " . $escrow->getId(),
            null,
            $escrow->getId()
        );

        // Increase trust score for receiver (seller)
        $receiverUser = $receiverWallet->getUser();
        $currentScore = $receiverUser->getTrustScore() ?? 100;
        $receiverUser->setTrustScore($currentScore + 10);
        $em->flush();
    }
}
