<?php

namespace App\Security;

use App\Entity\User;
use App\Entity\Wallet;
use App\Entity\VirtualCard;
use App\Entity\FinancialProfile;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Component\HttpFoundation\RedirectResponse;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Generator\UrlGeneratorInterface;
use Symfony\Component\Security\Core\Authentication\Token\TokenInterface;
use Symfony\Component\Security\Core\Exception\CustomUserMessageAuthenticationException;
use Symfony\Component\Security\Http\Authenticator\AbstractLoginFormAuthenticator;
use Symfony\Component\Security\Http\Authenticator\Passport\Badge\CsrfTokenBadge;
use Symfony\Component\Security\Http\Authenticator\Passport\Badge\UserBadge;
use Symfony\Component\Security\Http\Authenticator\Passport\Credentials\CustomCredentials;
use Symfony\Component\Security\Http\Authenticator\Passport\Passport;
use Symfony\Component\Security\Http\SecurityRequestAttributes;
use Symfony\Component\Security\Http\Util\TargetPathTrait;
use Symfony\Contracts\HttpClient\HttpClientInterface;

class FinHubAuthenticator extends AbstractLoginFormAuthenticator
{
    use TargetPathTrait;

    public const LOGIN_ROUTE = 'app_login';

    public function __construct(
        private UrlGeneratorInterface $urlGenerator,
        private HttpClientInterface $client,
        private EntityManagerInterface $entityManager,
        private string $authApiUrl
    ) {
    }

    public function authenticate(Request $request): Passport
    {
        $email = $request->request->get('email', '');
        $password = $request->request->get('password', '');

        $request->getSession()->set(SecurityRequestAttributes::LAST_USERNAME, $email);

        return new Passport(
            new UserBadge($email, function ($userIdentifier) use ($password, $request) {
                try {
                    // Call API to verify credentials
                    $response = $this->client->request('POST', $this->authApiUrl . '/login', [
                        'json' => [
                            'email' => $userIdentifier,
                            'password' => $password,
                        ],
                    ]);

                    if ($response->getStatusCode() !== 200) {
                        $errorMsg = 'Invalid credentials.';
                        if ($response->getStatusCode() >= 400 && $response->getStatusCode() < 500) {
                            $data = $response->toArray(false);
                            if (isset($data['detail'])) {
                                $errorMsg = is_array($data['detail']) ? json_encode($data['detail']) : $data['detail'];
                            } elseif (isset($data['message'])) {
                                $errorMsg = is_array($data['message']) ? json_encode($data['message']) : $data['message'];
                            }
                        }
                        throw new CustomUserMessageAuthenticationException($errorMsg);
                    }

                    $data = $response->toArray();
                    // Basic validation of response structure
                    if (!isset($data['user']['id'])) {
                        throw new CustomUserMessageAuthenticationException('Invalid API response.');
                    }

                    $userData = $data['user'];

                    // Sync to Local DB
                    // Explicitly fetch by ID if possible, or email
                    $user = $this->entityManager->getRepository(User::class)->find($userData['id']);
                    
                    if (!$user) {
                        $user = new User();
                        $user->setId($userData['id']);
                        $user->setTrustScore(100); // Default for new users if not provided
                    }

                    // Check if Admin
                    $isAdminRole = false;
                    $apiRole = strtoupper($userData['role'] ?? 'USER');
                    if ($apiRole === 'ROLE_ADMIN' || $apiRole === 'ADMIN') {
                        $isAdminRole = true;
                        $apiRole = 'ADMIN';
                    } elseif ($apiRole === 'ROLE_USER') {
                        $apiRole = 'USER';
                    }

                    // Update fields
                    $user->setEmail($userData['email']);
                    $user->setFullName($userData['full_name'] ?? 'Unknown');
                    $user->setRole($apiRole);
                    $user->setSyncedAt(new \DateTime('now', new \DateTimeZone('Africa/Tunis')));

                    // Store API token in session for future API requests
                    if (isset($data['access_token'])) {
                        $request->getSession()->set('api_token', $data['access_token']);
                    }

                    // Ensure Wallet Exists (ONLY FOR REGULAR USERS)
                    $wallet = $user->getWallet();
                    if (!$wallet && !$isAdminRole) {
                        $wallet = new Wallet();
                        $wallet->setUser($user);
                        $wallet->setBalance('0.00');
                        $wallet->setEscrowBalance('0.00');
                        $wallet->setCurrency('TND');
                        $wallet->setStatus('ACTIVE');
                        $user->setWallet($wallet);
                        $this->entityManager->persist($wallet);
                    }

                    // Ensure VirtualCard Exists (ONLY FOR REGULAR USERS)
                    if ($wallet && $wallet->getVirtualCards()->isEmpty() && !$isAdminRole) {
                        $card = new VirtualCard();
                        $card->setWallet($wallet);
                        
                        // Generate random 16 digit card number (for mock purposes)
                        $cardNumber = '';
                        for ($i = 0; $i < 16; $i++) {
                            $cardNumber .= mt_rand(0, 9);
                        }
                        
                        // Generate random 3 digit CVV
                        $cvv = str_pad((string) mt_rand(0, 999), 3, '0', STR_PAD_LEFT);
                        
                        $card->setCardNumber($cardNumber);
                        $card->setCvv($cvv);
                        $card->setExpiryDate((new \DateTime())->modify('+3 years'));
                        $card->setStatus('ACTIVE');
                        
                        $wallet->addVirtualCard($card);
                        $this->entityManager->persist($card);
                    }

                    // Ensure Financial Profile Exists (ONLY FOR REGULAR USERS)
                    $profile = $user->getFinancialProfile();
                    if (!$profile && !$isAdminRole) {
                        $profile = new FinancialProfile();
                        $profile->setUser($user);
                        $profile->setCurrency('TND');
                        $profile->setProfileCompleted(false);
                        $user->setFinancialProfile($profile);
                        $this->entityManager->persist($profile);
                    }

                    $this->entityManager->persist($user);
                    $this->entityManager->flush();

                    return $user;

                } catch (\Exception $e) {
                    if ($e instanceof CustomUserMessageAuthenticationException) {
                        throw $e;
                    }
                    // Log generic error?
                    throw new CustomUserMessageAuthenticationException('Authentication service unavailable.');
                }
            }),
            new CustomCredentials(
                function ($credentials, User $user) {
                    return true; // We already verified via API
                },
                $password
            ),
            [
                new CsrfTokenBadge('authenticate', $request->request->get('_csrf_token')),
            ]
        );
    }

    public function onAuthenticationSuccess(Request $request, TokenInterface $token, string $firewallName): ?Response
    {
        if ($targetPath = $this->getTargetPath($request->getSession(), $firewallName)) {
            return new RedirectResponse($targetPath);
        }

        $roles = $token->getRoleNames();
        $isAdmin = false;
        foreach ($roles as $role) {
            $roleUpper = strtoupper($role);
            if ($roleUpper === 'ROLE_ADMIN' || $roleUpper === 'ADMIN') {
                $isAdmin = true;
                break;
            }
        }

        /** @var User $user */
        $user = $token->getUser();

        if ($isAdmin) {
            // Check if admin has a wallet and needs to migrate
            if ($user->getWallet()) {
                return new RedirectResponse($this->urlGenerator->generate('app_admin_migrate_wallet'));
            }
            return new RedirectResponse($this->urlGenerator->generate('app_admin_dashboard'));
        }

        $profile = $user->getFinancialProfile();
        
        if ($profile && !$profile->isProfileCompleted()) {
            return new RedirectResponse($this->urlGenerator->generate('app_complete_profile'));
        }

        // Redirect to user dashboard based on role
        return new RedirectResponse($this->urlGenerator->generate('app_user_dashboard'));
    }

    protected function getLoginUrl(Request $request): string
    {
        return $this->urlGenerator->generate(self::LOGIN_ROUTE);
    }
}
