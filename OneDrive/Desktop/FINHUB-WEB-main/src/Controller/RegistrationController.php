<?php

namespace App\Controller;

use App\Entity\User;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Annotation\Route;
use Symfony\Contracts\HttpClient\HttpClientInterface;
use Symfony\Component\Security\Http\Authentication\UserAuthenticatorInterface;
use App\Security\FinHubAuthenticator;
use Symfony\Component\Mailer\MailerInterface;
use Symfony\Bridge\Twig\Mime\TemplatedEmail;
use Symfony\Component\Mime\Address;
use App\Form\RegistrationType;
use Symfony\Component\Form\FormError;
use Symfony\Component\DependencyInjection\Attribute\Autowire;

class RegistrationController extends AbstractController
{
    #[Route('/register', name: 'app_register')]
    public function register(
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
        
        $form = $this->createForm(RegistrationType::class);
        $form->handleRequest($request);

        if ($form->isSubmitted() && $form->isValid()) {
            $dataForm = $form->getData();
            $email = $dataForm['email'];
            $password = $dataForm['password'];
            $fullName = $dataForm['fullName'];
            
            try {
                // Call API to Register
                $response = $client->request('POST', $authApiUrl . '/signup', [
                     'json' => [
                        'email' => $email,
                        'password' => $password,
                        'full_name' => $fullName,
                        'role' => 'CLIENT'
                     ]
                ]);

                if ($response->getStatusCode() === 200 || $response->getStatusCode() === 201) {
                     $data = $response->toArray(false);
                     $verificationLink = $data['verification_link'] ?? null;

                     if ($verificationLink) {
                         $emailMessage = (new TemplatedEmail())
                             ->from(new Address('sadok.dridi.engineer@gmail.com', 'FINHUB Security'))
                             ->to($email)
                             ->subject('Verify your FINHUB email address')
                             ->htmlTemplate('emails/verification_email.html.twig')
                             ->context([
                                 'verification_link' => $verificationLink,
                             ]);

                         try {
                             $mailer->send($emailMessage);
                         } catch (\Exception $e) {
                             $this->addFlash('error', 'Account created, but failed to send verification email.');
                         }
                     }

                     $request->getSession()->set('registered_email', $email);
                     return $this->redirectToRoute('app_check_email');
                } else {
                     $data = $response->toArray(false);
                     $errorData = $data['detail'] ?? ($data['message'] ?? 'Registration failed.');

                     if (is_array($errorData)) {
                         // Map Pydantic validation array errors to specific form fields
                         foreach ($errorData as $err) {
                             if (is_array($err) && isset($err['loc']) && isset($err['msg'])) {
                                 $field = end($err['loc']); // 'email', 'password', 'full_name'
                                 if ($field === 'full_name') $field = 'fullName';

                                 if ($form->has($field)) {
                                     $form->get($field)->addError(new FormError($err['msg']));
                                 } else {
                                     $error = $err['msg']; // Fallback to global error
                                 }
                             } else if (is_array($err) && isset($err['msg'])) {
                                 $error = $err['msg'];
                             } else {
                                 $error = is_string($err) ? $err : json_encode($err);
                             }
                         }
                     } else {
                         // Standard global string error
                         $error = $errorData;
                     }
                }

            } catch (\Exception $e) {
                $error = 'An error occurred: ' . $e->getMessage();
            }
        }

        $status = ($error || ($form->isSubmitted() && !$form->isValid())) ? Response::HTTP_UNPROCESSABLE_ENTITY : Response::HTTP_OK;

        return $this->render('registration/register.html.twig', [
            'error' => $error,
            'form' => $form->createView()
        ], new Response(null, $status));
    }

    #[Route('/check-email', name: 'app_check_email')]
    public function checkEmail(Request $request): Response
    {
        $email = $request->getSession()->get('registered_email');
        
        if (!$email) {
            return $this->redirectToRoute('app_register');
        }

        // Optional: clear the email from session so refresh redirects back, or keep it.
        // $request->getSession()->remove('registered_email');

        return $this->render('registration/check_email.html.twig', [
            'email' => $email,
        ]);
    }
}
