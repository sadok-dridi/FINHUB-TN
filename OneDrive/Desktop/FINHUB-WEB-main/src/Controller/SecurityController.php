<?php

namespace App\Controller;

use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Annotation\Route;
use Symfony\Component\Security\Http\Authentication\AuthenticationUtils;
use Symfony\Component\Form\FormFactoryInterface;
use Symfony\Component\Form\FormError;
use App\Form\LoginType;

class SecurityController extends AbstractController
{
    #[Route(path: '/login', name: 'app_login')]
    public function login(AuthenticationUtils $authenticationUtils, FormFactoryInterface $formFactory): Response
    {
        if ($this->getUser()) {
            return $this->redirectToRoute('app_home');
        }

        // get the login error if there is one
        $authError = $authenticationUtils->getLastAuthenticationError();
        // last username entered by the user
        $lastUsername = $authenticationUtils->getLastUsername();

        $form = $formFactory->createNamed('', LoginType::class, ['email' => $lastUsername]);
        
        $globalError = null;

        if ($authError) {
            $errorMsg = $authError->getMessage(); // Get the raw message from Authenticator
            $decoded = json_decode($errorMsg, true);
            
            if (json_last_error() === JSON_ERROR_NONE && is_array($decoded)) {
                // It's a JSON array (Pydantic validation error)
                foreach ($decoded as $err) {
                    if (is_array($err) && isset($err['loc']) && isset($err['msg'])) {
                        $field = end($err['loc']); // e.g., 'email', 'password'
                        
                        if ($form->has($field)) {
                            // Inject error specifically under the input field!
                            $form->get($field)->addError(new FormError($err['msg']));
                        } else {
                            $globalError = $err['msg'];
                        }
                    } elseif (is_array($err) && isset($err['msg'])) {
                        $globalError = $err['msg'];
                    } else {
                        $globalError = is_string($err) ? $err : json_encode($err);
                    }
                }
            } else {
                // It's a standard string error (e.g., "Invalid credentials.")
                $globalError = $errorMsg;
            }
        }

        return $this->render('security/login.html.twig', [
            'last_username' => $lastUsername, 
            'error' => $globalError,
            'form' => $form->createView()
        ]);
    }

    #[Route(path: '/logout', name: 'app_logout')]
    public function logout(): void
    {
        throw new \LogicException('This method can be blank - it will be intercepted by the logout key on your firewall.');
    }
}
