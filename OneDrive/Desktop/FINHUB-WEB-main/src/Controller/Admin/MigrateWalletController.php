<?php

namespace App\Controller\Admin;

use App\Entity\User;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Annotation\Route;
use Symfony\Component\Security\Http\Attribute\IsGranted;
use Symfony\Contracts\HttpClient\HttpClientInterface;
use Symfony\Component\Mailer\MailerInterface;
use Symfony\Bridge\Twig\Mime\TemplatedEmail;
use Symfony\Component\Mime\Address;

#[IsGranted('ROLE_ADMIN')]
class MigrateWalletController extends AbstractController
{
    #[Route('/admin/migrate-wallet', name: 'app_admin_migrate_wallet')]
    public function index(): Response
    {
        /** @var User $user */
        $user = $this->getUser();

        // If they don't have a wallet, they don't need to migrate
        if (!$user->getWallet()) {
            return $this->redirectToRoute('app_admin_dashboard');
        }

        return $this->render('admin/migrate_wallet.html.twig');
    }

    #[Route('/admin/migrate-wallet/init', name: 'app_admin_migrate_wallet_init', methods: ['POST'])]
    public function initMigration(Request $request, HttpClientInterface $client, MailerInterface $mailer): JsonResponse
    {
        $data = json_decode($request->getContent(), true);
        $name = trim($data['fullName'] ?? '');
        $email = trim($data['email'] ?? '');
        $password = $data['password'] ?? '';

        if (empty($name) || empty($email) || empty($password)) {
            return new JsonResponse(['success' => false, 'message' => 'All fields are required.'], 400);
        }

        try {
            // 1. Call API to sign up the new user (this returns the verification link)
            $response = $client->request('POST', 'https://api.finhub.tn/signup', [
                'json' => [
                    'full_name' => $name,
                    'email' => $email,
                    'password' => $password,
                ],
            ]);

            if ($response->getStatusCode() >= 400) {
                $errorData = $response->toArray(false);
                $errorMsg = $errorData['detail'] ?? ($errorData['message'] ?? 'Signup Error');
                return new JsonResponse(['success' => false, 'message' => $errorMsg], 400);
            }

            $responseData = $response->toArray();
            $verificationLink = $responseData['verification_link'] ?? null;

            if (!$verificationLink) {
                return new JsonResponse(['success' => false, 'message' => 'API did not return a verification link.'], 500);
            }

            // 2. Send the verification email to the user
            $emailMessage = (new TemplatedEmail())
                ->from(new Address('sadok.dridi.engineer@gmail.com', 'FINHUB Security'))
                ->to($email)
                ->subject('Verify your FINHUB email address')
                ->htmlTemplate('emails/verification.html.twig')
                ->context([
                    'verification_link' => $verificationLink,
                    'user_name' => $name
                ]);

            $mailer->send($emailMessage);

            return new JsonResponse([
                'success' => true,
                'email' => $email,
                'message' => 'Verification email sent successfully.'
            ]);

        } catch (\Exception $e) {
            return new JsonResponse(['success' => false, 'message' => 'Error: ' . $e->getMessage()], 500);
        }
    }

    #[Route('/admin/migrate-wallet/finalize', name: 'app_admin_migrate_wallet_finalize', methods: ['POST'])]
    public function finalizeMigration(Request $request, HttpClientInterface $client, EntityManagerInterface $em): JsonResponse
    {
        /** @var User $adminUser */
        $adminUser = $this->getUser();
        $wallet = $adminUser->getWallet();

        if (!$wallet) {
            return new JsonResponse(['success' => false, 'message' => 'No wallet to migrate.'], 400);
        }

        $data = json_decode($request->getContent(), true);
        $email = trim($data['email'] ?? '');
        $password = $data['password'] ?? '';

        if (empty($email) || empty($password)) {
            return new JsonResponse(['success' => false, 'message' => 'Email and password required to finalize.'], 400);
        }

        try {
            // 3. Login to verify the email was verified and fetch user data
            $loginResponse = $client->request('POST', 'https://api.finhub.tn/login', [
                'json' => [
                    'email' => $email,
                    'password' => $password,
                ],
            ]);

            if ($loginResponse->getStatusCode() !== 200) {
                $errorData = $loginResponse->toArray(false);
                $errorMsg = $errorData['detail'] ?? ($errorData['message'] ?? 'Login Error');
                return new JsonResponse([
                    'success' => false, 
                    'message' => "Verification incomplete or invalid credentials: " . $errorMsg
                ], 400);
            }

            $loginData = $loginResponse->toArray();
            $newUserId = $loginData['user']['id'];

            // 4. Create the user in the local database to satisfy FK constraints
            $newUser = $em->getRepository(User::class)->find($newUserId);
            if (!$newUser) {
                $newUser = new User();
                $newUser->setId($newUserId);
            }
            $newUser->setEmail($loginData['user']['email']);
            $newUser->setFullName($loginData['user']['full_name'] ?? 'Unknown');
            $newUser->setRole('USER');
            $newUser->setSyncedAt(new \DateTime('now', new \DateTimeZone('Africa/Tunis')));
            $em->persist($newUser);
            $em->flush();

            // 5. Transfer the wallet and financial profile
            $wallet->setUser($newUser);
            $profile = $adminUser->getFinancialProfile();
            if ($profile) {
                $profile->setUser($newUser);
            }

            $em->flush();

            return new JsonResponse([
                'success' => true, 
                'message' => "Your wallet and profile have been transferred to {$email}.\n\nYou may now access the Admin Dashboard."
            ]);

        } catch (\Exception $e) {
            return new JsonResponse(['success' => false, 'message' => 'Error finalizing migration: ' . $e->getMessage()], 500);
        }
    }
}
