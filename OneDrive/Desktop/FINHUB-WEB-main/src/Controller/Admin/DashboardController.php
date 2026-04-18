<?php

namespace App\Controller\Admin;

use App\Entity\User;
use App\Entity\KycRequest;
use App\Entity\SupportTicket;
use App\Entity\Escrow;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Annotation\Route;
use Symfony\Component\Security\Http\Attribute\IsGranted;

#[IsGranted('ROLE_ADMIN')]
class DashboardController extends AbstractController
{
    #[Route('/admin', name: 'app_admin_dashboard')]
    public function index(EntityManagerInterface $em): Response
    {
        /** @var User $user */
        $user = $this->getUser();

        // Fetch real stats from the database mimicking the Java app logic
        $totalUsers = $em->getRepository(User::class)->count([]);
        
        $pendingVerifications = $em->getRepository(KycRequest::class)->count(['status' => 'PENDING']);
        
        $openTickets = $em->getRepository(SupportTicket::class)->count(['status' => 'OPEN']);
        
        // Calculate Active Escrow Volume
        $escrowVolumeResult = $em->createQueryBuilder()
            ->select('SUM(e.amount)')
            ->from(Escrow::class, 'e')
            ->where('e.status IN (:statuses)')
            ->setParameter('statuses', ['LOCKED', 'DISPUTED'])
            ->getQuery()
            ->getSingleScalarResult();
            
        $escrowVolume = $escrowVolumeResult ? number_format((float)$escrowVolumeResult, 2, '.', '') : '0.00';

        $stats = [
            'totalUsers' => $totalUsers,
            'pendingVerifications' => $pendingVerifications,
            'openTickets' => $openTickets,
            'escrowVolume' => $escrowVolume . ' TND',
        ];

        return $this->render('admin/dashboard.html.twig', [
            'user' => $user,
            'stats' => $stats,
        ]);
    }
}
