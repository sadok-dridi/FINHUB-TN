<?php

namespace App\Controller\User;

use App\Entity\KycRequest;
use App\Entity\User;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Annotation\Route;
use Symfony\Component\Security\Http\Attribute\IsGranted;
use Cloudinary\Cloudinary;
use Cloudinary\Configuration\Configuration;
use App\Form\KycRequestType;

#[IsGranted('ROLE_USER')]
class KycController extends AbstractController
{
    #[Route('/kyc', name: 'app_user_kyc')]
    public function index(Request $request, EntityManagerInterface $entityManager): Response
    {
        /** @var User $user */
        $user = $this->getUser();

        // Check if they already have an approved or pending request
        $kycRequests = $user->getKycRequests();
        foreach ($kycRequests as $req) {
            $st = strtoupper($req->getStatus());
            if ($st === 'PENDING') {
                $this->addFlash('info', 'You already have a KYC request pending review.');
                return $this->redirectToRoute('app_user_my_profile');
            }
            if ($st === 'APPROVED') {
                $this->addFlash('info', 'Your identity is already verified.');
                return $this->redirectToRoute('app_user_my_profile');
            }
        }

        $kycRequest = new KycRequest();
        $form = $this->createForm(KycRequestType::class, $kycRequest);
        $form->handleRequest($request);

        if ($form->isSubmitted() && $form->isValid()) {
            $file = $form->get('document_file')->getData();

            if ($file) {
                try {
                    // Upload to Cloudinary to match Java App's CloudinaryService
                    $cloudinaryUrl = $_ENV['CLOUDINARY_URL'] ?? null;
                    if (!$cloudinaryUrl) {
                        throw new \Exception("CLOUDINARY_URL is not set in environment variables");
                    }
                    
                    $cloudinary = new Cloudinary($cloudinaryUrl);
                    
                    $folder = "kyc/" . strtolower($kycRequest->getDocumentType());
                    
                    $uploadResult = $cloudinary->uploadApi()->upload(
                        $file->getPathname(),
                        [
                            'folder' => $folder,
                            'resource_type' => 'auto'
                        ]
                    );
                    
                    $secureUrl = $uploadResult['secure_url'];

                    $kycRequest->setUser($user);
                    $kycRequest->setDocumentUrl($secureUrl);
                    $kycRequest->setStatus('PENDING');
                    $kycRequest->setSubmissionDate(new \DateTime('now', new \DateTimeZone('Africa/Tunis')));
                    
                    $entityManager->persist($kycRequest);
                    $entityManager->flush();

                    $this->addFlash('success', 'Your verification request has been submitted successfully.');
                    return $this->redirectToRoute('app_user_my_profile');

                } catch (\Exception $e) {
                    $this->addFlash('error', 'There was an error uploading your document: ' . $e->getMessage());
                }
            } else {
                $this->addFlash('error', 'Please provide a valid document.');
            }
        }

        $status = ($form->isSubmitted() && !$form->isValid()) ? Response::HTTP_UNPROCESSABLE_ENTITY : Response::HTTP_OK;

        return $this->render('user/kyc_submission.html.twig', [
            'user' => $user,
            'form' => $form->createView()
        ], new Response(null, $status));
    }
}

