<?php

namespace App\Controller;

use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Annotation\Route;
use Symfony\Contracts\HttpClient\HttpClientInterface;
use Symfony\Component\Mailer\MailerInterface;
use Symfony\Bridge\Twig\Mime\TemplatedEmail;
use Symfony\Component\Mime\Address;
use Symfony\Component\DependencyInjection\Attribute\Autowire;

class ForgotPasswordController extends AbstractController
{
    #[Route('/forgot-password', name: 'app_forgot_password')]
    public function forgotPassword(
        Request $request,
        HttpClientInterface $client,
        MailerInterface $mailer,
        #[Autowire('%env(AUTH_API_URL)%')] string $authApiUrl
    ): Response
    {
        if ($this->getUser()) {
             return $this->redirectToRoute('app_home');
        }

        $error = null;
        $success = null;

        if ($request->isMethod('POST')) {
            $email = $request->request->get('email');
            
            try {
                // Request Reset Link from API
                $response = $client->request('POST', $authApiUrl . '/forgot-password', [
                     'json' => [
                        'email' => $email
                     ]
                ]);

                if ($response->getStatusCode() === 200 || $response->getStatusCode() === 201) {
                     $data = $response->toArray(false);
                     $resetLink = $data['reset_link'] ?? null;

                     if ($resetLink) {
                         // Send Email
                         $emailMessage = (new TemplatedEmail())
                             ->from(new Address('sadok.dridi.engineer@gmail.com', 'FINHUB Security'))
                             ->to($email)
                             ->subject('Reset your FINHUB password')
                             ->htmlTemplate('emails/reset_password.html.twig')
                             ->context([
                                 'reset_link' => $resetLink,
                             ]);

                         try {
                             $mailer->send($emailMessage);
                             $success = 'Reset link sent! Check your email.';
                         } catch (\Exception $e) {
                             $error = 'Error sending email: ' . $e->getMessage();
                         }
                     } else {
                         // Some API fallback case
                         $success = 'Reset link sent! Check your email.'; 
                     }
                } else {
                     $data = $response->toArray(false);
                     $error = $data['detail'] ?? ($data['message'] ?? 'Failed to send reset link.');
                }

            } catch (\Exception $e) {
                $error = 'An error occurred: ' . $e->getMessage();
            }
        }

        return $this->render('security/forgot_password.html.twig', [
            'error' => $error,
            'success' => $success,
        ]);
    }
}