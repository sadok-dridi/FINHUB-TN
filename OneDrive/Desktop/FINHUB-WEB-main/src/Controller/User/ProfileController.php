<?php

namespace App\Controller\User;

use App\Entity\User;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Annotation\Route;
use Symfony\Component\Security\Http\Attribute\IsGranted;
use Cloudinary\Cloudinary;
use App\Form\FinancialProfileType;

#[IsGranted('ROLE_USER')]
class ProfileController extends AbstractController
{
    #[Route('/complete-profile', name: 'app_complete_profile')]
    public function completeProfile(Request $request, EntityManagerInterface $entityManager): Response
    {
        /** @var User $user */
        $user = $this->getUser();
        
        // If an admin tries to access this, redirect them to admin dashboard
        if ($this->isGranted('ROLE_ADMIN')) {
            return $this->redirectToRoute('app_admin_dashboard');
        }

        $profile = $user->getFinancialProfile() ?? new \App\Entity\FinancialProfile();
        $profile->setUser($user);

        if ($profile->isProfileCompleted()) {
            return $this->redirectToRoute('app_user_dashboard');
        }

        $form = $this->createForm(FinancialProfileType::class, $profile);
        
        // Populate the unmapped phone number if it exists
        if ($user->getPhoneNumber()) {
            $form->get('phone_number')->setData($user->getPhoneNumber());
        }

        $form->handleRequest($request);

        if ($form->isSubmitted() && $form->isValid()) {
            $profile->setProfileCompleted(true);
            
            // Handle the unmapped phone number
            $phone = $form->get('phone_number')->getData();
            if ($phone) {
                $user->setPhoneNumber($phone);
            }

            $entityManager->persist($profile);
            $entityManager->flush();
            
            $this->addFlash('success', 'Profile completed successfully! Welcome to your dashboard.');
            return $this->redirectToRoute('app_user_dashboard');
        }

        $status = ($form->isSubmitted() && !$form->isValid()) ? Response::HTTP_UNPROCESSABLE_ENTITY : Response::HTTP_OK;

        return $this->render('user/complete_profile.html.twig', [
            'user' => $user,
            'form' => $form->createView(),
        ], new Response(null, $status));
    }

    #[Route('/profile', name: 'app_user_my_profile')]
    public function profile(Request $request, EntityManagerInterface $entityManager): Response
    {
        /** @var User $user */
        $user = $this->getUser();
        
        // If an admin tries to access this, redirect them to admin dashboard
        if ($this->isGranted('ROLE_ADMIN')) {
            return $this->redirectToRoute('app_admin_dashboard');
        }

        $profile = $user->getFinancialProfile() ?? new \App\Entity\FinancialProfile();
        $kycRequests = $user->getKycRequests();
        
        $isPending = false;
        $isVerified = false;
        $isRejected = false;
        
        foreach ($kycRequests as $req) {
            $st = strtoupper($req->getStatus());
            if ($st === 'PENDING') $isPending = true;
            if ($st === 'APPROVED') $isVerified = true;
            if ($st === 'REJECTED') $isRejected = true;
        }

        $form = $this->createForm(FinancialProfileType::class, $profile);

        $form->get('full_name')->setData($user->getFullName());

        // Populate the unmapped phone number if it exists
        if ($user->getPhoneNumber()) {
            $form->get('phone_number')->setData($user->getPhoneNumber());
        }

        $form->handleRequest($request);

        if ($form->isSubmitted() && $form->isValid()) {
            if (!$profile->getId()) {
                 $profile->setUser($user);
            }
            $profile->setUpdatedAt(new \DateTime('now', new \DateTimeZone('Africa/Tunis')));
            $profile->setProfileCompleted(true);

            $user->setFullName(trim((string) $form->get('full_name')->getData()));
            
            // Handle the unmapped phone number
            $phone = $form->get('phone_number')->getData();
            if ($phone) {
                $user->setPhoneNumber($phone);
            }

            $entityManager->persist($profile);
            $entityManager->flush();
            
            $this->addFlash('success', 'Profile updated successfully.');
            return $this->redirectToRoute('app_user_my_profile');
        }

        $status = ($form->isSubmitted() && !$form->isValid()) ? Response::HTTP_UNPROCESSABLE_ENTITY : Response::HTTP_OK;

        return $this->render('user/profile.html.twig', [
            'user' => $user,
            'financialProfile' => $profile,
            'isVerified' => $isVerified,
            'isPending' => $isPending,
            'isRejected' => $isRejected,
            'form' => $form->createView()
        ], new Response(null, $status));
    }

    #[Route('/profile/photo/upload', name: 'app_user_profile_photo_upload', methods: ['POST'])]
    public function uploadPhoto(Request $request, EntityManagerInterface $entityManager): Response
    {
        /** @var User $user */
        $user = $this->getUser();
        $file = $request->files->get('profile_photo');

        if ($file) {
            try {
                $cloudinaryUrl = $_ENV['CLOUDINARY_URL'] ?? null;
                if (!$cloudinaryUrl) {
                    throw new \Exception("CLOUDINARY_URL is not set in environment variables");
                }
                
                $cloudinary = new Cloudinary($cloudinaryUrl);
                
                $uploadResult = $cloudinary->uploadApi()->upload(
                    $file->getPathname(),
                    [
                        'folder' => 'profiles',
                        'resource_type' => 'image'
                    ]
                );
                
                $secureUrl = $uploadResult['secure_url'];

                $user->setProfilePhotoUrl($secureUrl);
                $entityManager->flush();

                $this->addFlash('success', 'Profile photo updated successfully!');
            } catch (\Exception $e) {
                $this->addFlash('error', 'Failed to upload photo: ' . $e->getMessage());
            }
        } else {
            $this->addFlash('error', 'No file provided.');
        }

        return $this->redirectToRoute('app_user_my_profile');
    }

    #[Route('/profile/photo/delete', name: 'app_user_profile_photo_delete', methods: ['POST'])]
    public function deletePhoto(EntityManagerInterface $entityManager): Response
    {
        /** @var User $user */
        $user = $this->getUser();

        if ($user->getProfilePhotoUrl()) {
            $user->setProfilePhotoUrl(null);
            $entityManager->flush();
            $this->addFlash('success', 'Profile photo deleted successfully.');
        }

        return $this->redirectToRoute('app_user_my_profile');
    }
}
