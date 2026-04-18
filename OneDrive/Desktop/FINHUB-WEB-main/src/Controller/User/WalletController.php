<?php

namespace App\Controller\User;

use App\Entity\User;
use App\Service\WalletService;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bridge\Twig\Mime\TemplatedEmail;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Mailer\MailerInterface;
use Symfony\Component\Routing\Annotation\Route;
use Symfony\Component\Security\Http\Attribute\IsGranted;

use App\Form\TransferType;
use Symfony\Component\Form\FormFactoryInterface;

use App\Form\CashInType;
use App\Form\CardCashInType;

#[IsGranted('ROLE_USER')]
class WalletController extends AbstractController
{
    #[Route('/wallet/transfer/init', name: 'app_wallet_transfer_init', methods: ['POST'])]
    public function transferInit(Request $request, EntityManagerInterface $em, MailerInterface $mailer, FormFactoryInterface $formFactory): JsonResponse
    {
        /** @var User $user */
        $user = $this->getUser();
        $senderWallet = $user->getWallet();

        if (!$senderWallet) {
            return new JsonResponse(['success' => false, 'message' => 'You do not have an active wallet.']);
        }
        
        if ($senderWallet->getStatus() === 'FROZEN') {
            return new JsonResponse(['success' => false, 'message' => 'Your wallet is frozen. Cannot send funds.']);
        }

        $data = json_decode($request->getContent(), true);

        $form = $formFactory->create(TransferType::class);
        $form->submit($data);

        $errors = [];

        if (!$form->isValid()) {
            foreach ($form->getErrors(true) as $error) {
                $fieldName = $error->getOrigin()->getName();
                if ($fieldName === 'recipientEmail' || $fieldName === 'amount') {
                    $errors[$fieldName] = $error->getMessage();
                }
            }
        }

        $recipientEmail = $form->get('recipientEmail')->getData() ?? '';
        $amountRaw = (float) ($form->get('amount')->getData() ?? 0);

        if (empty($errors)) {
            if ($recipientEmail === $user->getEmail()) {
                $errors['recipientEmail'] = 'You cannot transfer money to yourself.';
            } else {
                $recipientUser = $em->getRepository(User::class)->findOneBy(['email' => $recipientEmail]);
                if (!$recipientUser) {
                    $errors['recipientEmail'] = "User with email {$recipientEmail} not found.";
                } else {
                    $recipientWallet = $recipientUser->getWallet();
                    if (!$recipientWallet) {
                        $errors['recipientEmail'] = 'Recipient does not have an active wallet.';
                    } elseif ($recipientWallet->getStatus() === 'FROZEN') {
                        $errors['recipientEmail'] = 'Recipient wallet is frozen.';
                    }
                }
            }

            $amountStr = number_format($amountRaw, 3, '.', '');
            if ((float) $senderWallet->getBalance() < (float) $amountStr) {
                $errors['amount'] = 'Insufficient balance.';
            }
        }

        if (count($errors) > 0) {
            return new JsonResponse(['success' => false, 'errors' => $errors]);
        }

        try {
            // Generate Native Cryptographically Secure OTP
            $otpCode = (string) random_int(100000, 999999);
            
            // Send Email
            $emailInfo = (new TemplatedEmail())
                ->from('sadok.dridi.engineer@gmail.com') // Match Java Sender securely mapped
                ->to($user->getEmail()) // ALWAYS send OTP to the sender!
                ->subject('FINHUB Transaction Verification Code')
                ->htmlTemplate('emails/otp_email.html.twig')
                ->context(['otp' => $otpCode]);
                
            $mailer->send($emailInfo);

            // Store in Session
            $session = $request->getSession();
            $session->set('transfer_otp', $otpCode);
            $session->set('transfer_data', [
                'recipientEmail' => $recipientEmail,
                'amount' => $amountRaw,
                'expiresTime' => time() + 300 // 5 minutes expiration
            ]);

            return new JsonResponse(['success' => true, 'message' => 'OTP sent successfully.']);
            
        } catch (\Exception $e) {
            return new JsonResponse(['success' => false, 'message' => 'Failed to initialize transfer: ' . $e->getMessage()]);
        }
    }

    #[Route('/wallet/transfer/confirm', name: 'app_wallet_transfer_confirm', methods: ['POST'])]
    public function transferConfirm(Request $request, WalletService $walletService): JsonResponse
    {
        /** @var User $user */
        $user = $this->getUser();
        $senderWallet = $user->getWallet();

        $data = json_decode($request->getContent(), true);
        $submittedOtp = $data['otp'] ?? '';

        $session = $request->getSession();
        $expectedOtp = $session->get('transfer_otp');
        $transferData = $session->get('transfer_data');

        if (!$expectedOtp || !$transferData || time() > $transferData['expiresTime']) {
            $session->remove('transfer_otp');
            $session->remove('transfer_data');
            return new JsonResponse(['success' => false, 'message' => 'Session expired or invalid. Please try again.']);
        }

        if ($submittedOtp !== $expectedOtp) {
            return new JsonResponse(['success' => false, 'message' => 'Invalid verification code. Please try again.']); // Wait, just incorrect. Don't erase yet to allow retry? The Java app allows 3 retries or stays open. Let's stay open.
        }

        // Clear Session
        $session->remove('transfer_otp');
        $session->remove('transfer_data');

        try {
            // Execute the secure ledger transfer
            $result = $walletService->transferByEmail(
                $senderWallet->getId(), 
                $transferData['recipientEmail'], 
                $transferData['amount']
            );

            // Using Flash so when JS forces a reload, it pops up
            $this->addFlash('success', $result['message']);
            return new JsonResponse(['success' => true]);

        } catch (\Exception $e) {
            return new JsonResponse(['success' => false, 'message' => $e->getMessage()]);
        }
    }

    #[Route('/wallet/deposit/init', name: 'app_wallet_deposit_init', methods: ['POST'])]
    public function depositInit(Request $request, FormFactoryInterface $formFactory): JsonResponse
    {
        $data = json_decode($request->getContent(), true);

        $form = $formFactory->create(CashInType::class);
        $form->submit($data);

        $errors = [];

        if (!$form->isValid()) {
            foreach ($form->getErrors(true) as $error) {
                $fieldName = $error->getOrigin()->getName();
                if ($fieldName === 'amount') {
                    $errors[$fieldName] = $error->getMessage();
                }
            }
        }

        if (count($errors) > 0) {
            return new JsonResponse(['success' => false, 'errors' => $errors]);
        }

        return new JsonResponse(['success' => true]);
    }

    #[Route('/wallet/deposit/card/init', name: 'app_wallet_deposit_card_init', methods: ['POST'])]
    public function depositByCardInit(Request $request, FormFactoryInterface $formFactory, WalletService $walletService, MailerInterface $mailer): JsonResponse
    {
        /** @var User $user */
        $user = $this->getUser();
        $wallet = $user->getWallet();

        if (!$wallet) {
            return new JsonResponse(['success' => false, 'message' => 'You do not have an active wallet.']);
        }

        if ($wallet->getStatus() === 'FROZEN') {
            return new JsonResponse(['success' => false, 'message' => 'Your wallet is frozen. Cannot cash in funds.']);
        }

        $data = json_decode($request->getContent(), true) ?? [];
        $form = $formFactory->create(CardCashInType::class);
        $form->submit($data);

        $errors = [];
        if (!$form->isValid()) {
            foreach ($form->getErrors(true) as $error) {
                $fieldName = $error->getOrigin()->getName();
                $errors[$fieldName] = $error->getMessage();
            }
        }

        if (count($errors) > 0) {
            return new JsonResponse(['success' => false, 'errors' => $errors]);
        }

        try {
            $cardNumber = (string) $form->get('cardNumber')->getData();
            $expiryDate = (string) $form->get('expiryDate')->getData();
            $cvv = (string) $form->get('cvv')->getData();
            $amount = (float) $form->get('amount')->getData();

            $recipient = $walletService->getVirtualCardCashInOtpRecipient(
                $wallet->getId(),
                $cardNumber,
                $expiryDate,
                $cvv,
                $amount
            );

            $otpCode = (string) random_int(100000, 999999);

            $emailInfo = (new TemplatedEmail())
                ->from('sadok.dridi.engineer@gmail.com')
                ->to($recipient['email'])
                ->subject('FINHUB Transaction Verification Code')
                ->htmlTemplate('emails/otp_email.html.twig')
                ->context(['otp' => $otpCode]);

            $mailer->send($emailInfo);

            $session = $request->getSession();
            $session->set('card_cashin_otp', $otpCode);
            $session->set('card_cashin_data', [
                'cardNumber' => $cardNumber,
                'expiryDate' => $expiryDate,
                'cvv' => $cvv,
                'amount' => $amount,
                'expiresTime' => time() + 300,
            ]);

            $maskedEmail = preg_replace('/(^.).*(@.*$)/', '$1***$2', $recipient['email']) ?: $recipient['email'];

            return new JsonResponse([
                'success' => true,
                'message' => 'OTP sent successfully.',
                'maskedEmail' => $maskedEmail,
                'lastFour' => $recipient['lastFour'],
            ]);
        } catch (\Exception $e) {
            return new JsonResponse(['success' => false, 'message' => $e->getMessage()]);
        }
    }

    #[Route('/wallet/deposit/card/confirm', name: 'app_wallet_deposit_card_confirm', methods: ['POST'])]
    public function depositByCardConfirm(Request $request, WalletService $walletService): JsonResponse
    {
        /** @var User $user */
        $user = $this->getUser();
        $wallet = $user->getWallet();

        if (!$wallet) {
            return new JsonResponse(['success' => false, 'message' => 'You do not have an active wallet.']);
        }

        $data = json_decode($request->getContent(), true);
        $submittedOtp = $data['otp'] ?? '';

        $session = $request->getSession();
        $expectedOtp = $session->get('card_cashin_otp');
        $cashInData = $session->get('card_cashin_data');

        if (!$expectedOtp || !$cashInData || time() > $cashInData['expiresTime']) {
            $session->remove('card_cashin_otp');
            $session->remove('card_cashin_data');
            return new JsonResponse(['success' => false, 'message' => 'Session expired or invalid. Please try again.']);
        }

        if ($submittedOtp !== $expectedOtp) {
            return new JsonResponse(['success' => false, 'message' => 'Invalid verification code. Please try again.']);
        }

        $session->remove('card_cashin_otp');
        $session->remove('card_cashin_data');

        try {
            $result = $walletService->cashInByVirtualCard(
                $wallet->getId(),
                $cashInData['cardNumber'],
                $cashInData['expiryDate'],
                $cashInData['cvv'],
                (float) $cashInData['amount']
            );

            $this->addFlash('success', $result['message']);

            return new JsonResponse(['success' => true, 'message' => $result['message']]);
        } catch (\Exception $e) {
            return new JsonResponse(['success' => false, 'message' => $e->getMessage()]);
        }
    }

    // Classic Form Submit Fallback (kept just in case)
    #[Route('/wallet/transfer', name: 'app_wallet_transfer', methods: ['POST'])]
    public function transfer(Request $request, WalletService $walletService): Response
    {
        // ... (This isn't invoked by the JS flow, it's just a fallback if JS breaks).
        $this->addFlash('error', 'Please enable Javascript for Secure Two-Factor transfers.');
        return $this->redirectToRoute('app_user_dashboard');
    }

    #[Route('/wallet/balance', name: 'app_wallet_balance', methods: ['GET'])]
    public function currentBalance(): JsonResponse
    {
        /** @var User $user */
        $user = $this->getUser();
        $wallet = $user->getWallet();

        if (!$wallet) {
            return new JsonResponse(['success' => false, 'balance' => 0.0], 404);
        }

        // Just fetch the fresh balance from the memory cache / DB
        return new JsonResponse([
            'success' => true, 
            'walletId' => $wallet->getId(), 
            'userId' => $user->getId(),
            'balance' => (float) $wallet->getBalance()
        ]);
    }
}
