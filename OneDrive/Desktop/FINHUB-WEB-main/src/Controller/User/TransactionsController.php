<?php

namespace App\Controller\User;

use App\Entity\User;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Annotation\Route;
use Symfony\Component\Security\Http\Attribute\IsGranted;

use App\Form\ContactType;
use Symfony\Component\Form\FormFactoryInterface;

#[IsGranted('ROLE_USER')]
class TransactionsController extends AbstractController
{
    #[Route('/user/transactions', name: 'app_user_transactions')]
    public function index(\Symfony\Component\HttpFoundation\Request $request, EntityManagerInterface $entityManager, \App\Service\WalletService $walletService): Response
    {
        /** @var User $user */
        $user = $this->getUser();
        $wallet = $user->getWallet();

        $walletStatus = $wallet ? $wallet->getStatus() : 'NO WALLET';

        $rawTransactions = $wallet ? $wallet->getTransactions() : [];
        if ($rawTransactions instanceof \Doctrine\Common\Collections\Collection) {
            $rawTransactions = $rawTransactions->toArray();
        }

        $search = trim((string) $request->query->get('search', ''));
        $sort = (string) $request->query->get('sort', 'date_desc');

        if ($search !== '') {
            $searchNeedle = mb_strtolower($search);
            $rawTransactions = array_values(array_filter($rawTransactions, static function ($tx) use ($searchNeedle) {
                $haystack = implode(' ', [
                    (string) $tx->getReference(),
                    (string) $tx->getType(),
                    (string) $tx->getAmount(),
                ]);

                return str_contains(mb_strtolower($haystack), $searchNeedle);
            }));
        }

        usort($rawTransactions, static function ($a, $b) use ($sort) {
            return match ($sort) {
                'date_asc' => $a->getCreatedAt() <=> $b->getCreatedAt(),
                'amount_desc' => (float) $b->getAmount() <=> (float) $a->getAmount(),
                'amount_asc' => (float) $a->getAmount() <=> (float) $b->getAmount(),
                'type_asc' => strcasecmp((string) $a->getType(), (string) $b->getType()),
                'type_desc' => strcasecmp((string) $b->getType(), (string) $a->getType()),
                default => $b->getCreatedAt() <=> $a->getCreatedAt(),
            };
        });

        // 2. Pagination Logic (Calculate offset and slice BEFORE heavy DB lookups)
        $page = max(1, $request->query->getInt('page', 1));
        $limit = 10; // Increased to 10 since it's much faster now
        $totalItems = count($rawTransactions);
        $totalPages = max(1, ceil($totalItems / $limit));
        $page = min($page, $totalPages);
        $offset = ($page - 1) * $limit;
        
        // Only take the exact transactions we need for this page
        $paginatedRawTransactions = array_slice($rawTransactions, $offset, $limit);

        // Fetch Contacts matching Java App Logic
        $contacts = $entityManager->getRepository(\App\Entity\SavedContact::class)->findBy(['user' => $user], ['contactName' => 'ASC']);
        $enrichedContacts = [];
        
        // Simple memory cache to prevent duplicate user lookups in the loop
        $userCacheByEmail = [];
        $userCacheByName = [];

        foreach ($contacts as $contact) {
            $avatarUrl = null;
            $email = $contact->getContactEmail();
            $name = $contact->getContactName();
            $phone = $contact->getContactPhone();
            
            // First try to match by email
            if ($email) {
                if (!array_key_exists($email, $userCacheByEmail)) {
                    $userCacheByEmail[$email] = $entityManager->getRepository(User::class)->findOneBy(['email' => $email]);
                }
                $contactUser = $userCacheByEmail[$email];
                if ($contactUser && $contactUser->getProfilePhotoUrl()) {
                    $avatarUrl = $contactUser->getProfilePhotoUrl();
                }
            }
            
            // Fallback match by name
            if (!$avatarUrl && $name) {
                if (!array_key_exists($name, $userCacheByName)) {
                    $userCacheByName[$name] = $entityManager->getRepository(User::class)->findOneBy(['full_name' => $name]);
                }
                $contactUser = $userCacheByName[$name];
                if ($contactUser && $contactUser->getProfilePhotoUrl()) {
                    $avatarUrl = $contactUser->getProfilePhotoUrl();
                }
            }
            
            $enrichedContacts[] = [
                'id' => $contact->getId(),
                'name' => $contact->getContactName(),
                'email' => $contact->getContactEmail(),
                'phone' => $phone,
                'avatarUrl' => $avatarUrl,
                'initial' => strtoupper(substr(trim($contact->getContactName() ?? 'U'), 0, 1)),
            ];
        }

        $recentTransactions = [];

        // 3. Process ONLY the sliced transactions to avoid the N+1 Query Problem on all history
        foreach ($paginatedRawTransactions as $tx) {
            $ref = $tx->getReference() ?? '';
            $counterpartyName = 'System';
            $avatarUrl = null;

            if (preg_match('/(?:to|from) (.+?)(?: \((?:Wallet|User)|$)/i', $ref, $matches)) {
                $counterpartyName = trim($matches[1]);
                
                // Use cache to prevent hammering the DB if multiple tx exist for same person
                if (!array_key_exists($counterpartyName, $userCacheByName)) {
                    $userCacheByName[$counterpartyName] = $entityManager->getRepository(User::class)->findOneBy(['full_name' => $counterpartyName]);
                }
                
                $counterpartyUser = $userCacheByName[$counterpartyName];
                if ($counterpartyUser && $counterpartyUser->getProfilePhotoUrl()) {
                    $avatarUrl = $counterpartyUser->getProfilePhotoUrl();
                }
            }

            if (!$avatarUrl) {
                $seed = $counterpartyName === 'System' ? 'finhub' : urlencode($counterpartyName);
                $avatarUrl = 'https://api.dicebear.com/7.x/avataaars/svg?seed=' . $seed;
            }

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

        $contactForm = $this->createForm(ContactType::class);
        $editContactForm = $this->createForm(ContactType::class, null, [
            'include_email' => false,
            'include_name' => true,
            'include_phone' => true,
        ]);

        return $this->render('user/transactions.html.twig', [
            'recentTransactions' => $recentTransactions,
            'contacts' => $enrichedContacts,
            'currentPage' => $page,
            'totalPages' => $totalPages,
            'search' => $search,
            'sort' => $sort,
            'walletStatus' => $walletStatus,
            'contactForm' => $contactForm->createView(),
            'editContactForm' => $editContactForm->createView(),
        ]);
    }

    #[Route('/user/contacts/{id}/edit', name: 'app_user_contact_edit', methods: ['POST'])]
    public function editContact(int $id, \Symfony\Component\HttpFoundation\Request $request, EntityManagerInterface $em, FormFactoryInterface $formFactory): \Symfony\Component\HttpFoundation\JsonResponse
    {
        $user = $this->getUser();
        $contact = $em->getRepository(\App\Entity\SavedContact::class)->find($id);

        if (!$contact || $contact->getUser() !== $user) {
            return new \Symfony\Component\HttpFoundation\JsonResponse(['success' => false, 'message' => 'Contact not found.']);
        }

        $data = json_decode($request->getContent(), true) ?? [];

        $form = $formFactory->create(ContactType::class, null, [
            'include_email' => false,
            'include_name' => true,
            'include_phone' => true,
        ]);
        $form->submit($data);

        $errors = [];
        if (!$form->isValid()) {
            foreach ($form->getErrors(true) as $error) {
                $fieldName = $error->getOrigin()->getName();
                $errors[$fieldName] = $error->getMessage();
            }

            return new \Symfony\Component\HttpFoundation\JsonResponse(['success' => false, 'errors' => $errors]);
        }

        $contact->setContactName(trim((string) $form->get('name')->getData()));

        $phone = trim((string) $form->get('phone')->getData());
        $contact->setContactPhone($phone !== '' ? $phone : null);
        $em->flush();

        return new \Symfony\Component\HttpFoundation\JsonResponse(['success' => true]);
    }

    #[Route('/user/contacts/{id}/delete', name: 'app_user_contact_delete', methods: ['POST'])]
    public function deleteContact(int $id, EntityManagerInterface $em): \Symfony\Component\HttpFoundation\JsonResponse
    {
        $user = $this->getUser();
        $contact = $em->getRepository(\App\Entity\SavedContact::class)->find($id);

        if (!$contact || $contact->getUser() !== $user) {
            return new \Symfony\Component\HttpFoundation\JsonResponse(['success' => false, 'message' => 'Contact not found.']);
        }

        $em->remove($contact);
        $em->flush();

        return new \Symfony\Component\HttpFoundation\JsonResponse(['success' => true]);
    }

    #[Route('/user/contacts/add', name: 'app_user_contact_add', methods: ['POST'])]
    public function addContact(\Symfony\Component\HttpFoundation\Request $request, EntityManagerInterface $em, FormFactoryInterface $formFactory): \Symfony\Component\HttpFoundation\JsonResponse
    {
        /** @var User $user */
        $user = $this->getUser();
        $data = json_decode($request->getContent(), true);

        $form = $formFactory->create(ContactType::class);
        $form->submit($data);

        $errors = [];

        if (!$form->isValid()) {
            foreach ($form->getErrors(true) as $error) {
                $fieldName = $error->getOrigin()->getName();
                if ($fieldName === 'email') {
                    $errors[$fieldName] = $error->getMessage();
                }
            }
        }

        $email = $form->get('email')->getData() ?? '';

        if (empty($errors)) {
            // Check if contact already exists for this user
            $existing = $em->getRepository(\App\Entity\SavedContact::class)->findOneBy(['user' => $user, 'contactEmail' => $email]);
            if ($existing) {
                $errors['email'] = 'Contact already exists.';
            } elseif ($email === $user->getEmail()) {
                $errors['email'] = 'You cannot add yourself as a contact.';
            }
        }

        if (count($errors) > 0) {
            return new \Symfony\Component\HttpFoundation\JsonResponse(['success' => false, 'errors' => $errors]);
        }

        // Fetch user by email to get their name, fallback to part before @
        $contactUser = $em->getRepository(User::class)->findOneBy(['email' => $email]);
        $name = $contactUser && $contactUser->getFullName() ? $contactUser->getFullName() : explode('@', $email)[0];

        $contact = new \App\Entity\SavedContact();
        $contact->setUser($user);
        $contact->setContactEmail($email);
        $contact->setContactName($name);
        $contact->setContactPhone($contactUser?->getPhoneNumber());
        
        $em->persist($contact);
        $em->flush();

        return new \Symfony\Component\HttpFoundation\JsonResponse(['success' => true]);
    }
}
