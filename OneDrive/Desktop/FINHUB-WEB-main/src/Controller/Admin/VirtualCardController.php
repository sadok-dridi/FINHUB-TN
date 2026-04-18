<?php

namespace App\Controller\Admin;

use App\Entity\VirtualCard;
use App\Form\AdminVirtualCardType;
use App\Repository\VirtualCardRepository;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Annotation\Route;
use Symfony\Component\Security\Http\Attribute\IsGranted;

#[IsGranted('ROLE_ADMIN')]
class VirtualCardController extends AbstractController
{
    #[Route('/admin/virtual-cards', name: 'app_admin_virtual_cards')]
    public function index(Request $request, VirtualCardRepository $virtualCardRepository): Response
    {
        $connection = $virtualCardRepository->getEntityManager()->getConnection();
        $search = trim((string) $request->query->get('search', ''));
        $sort = (string) $request->query->get('sort', 'name');
        $direction = strtolower((string) $request->query->get('direction', 'asc')) === 'desc' ? 'DESC' : 'ASC';
        $page = max(1, $request->query->getInt('page', 1));
        $perPage = 12;

        $sortMap = [
            'name' => 'COALESCE(u.full_name, u.email, \'\')',
            'status' => 'vc.status',
            'expiry' => 'vc.expiry_date',
        ];
        $sort = array_key_exists($sort, $sortMap) ? $sort : 'name';

        $baseQb = $connection->createQueryBuilder()
            ->from('virtual_cards', 'vc')
            ->leftJoin('vc', 'wallets', 'w', 'w.id = vc.wallet_id')
            ->leftJoin('w', 'users_local', 'u', 'u.user_id = w.user_id');

        $stats = $connection->fetchAssociative(
            "SELECT COUNT(*) AS total_cards,
                    SUM(CASE WHEN status = 'ACTIVE' THEN 1 ELSE 0 END) AS active_cards,
                    SUM(CASE WHEN status = 'FROZEN' THEN 1 ELSE 0 END) AS frozen_cards,
                    SUM(CASE WHEN status = 'EXPIRED' THEN 1 ELSE 0 END) AS expired_cards
             FROM virtual_cards"
        ) ?: [
            'total_cards' => 0,
            'active_cards' => 0,
            'frozen_cards' => 0,
            'expired_cards' => 0,
        ];

        if ($search !== '') {
            $baseQb
                ->andWhere("LOWER(COALESCE(u.full_name, '')) LIKE :search OR LOWER(COALESCE(u.email, '')) LIKE :search OR vc.card_number LIKE :searchRaw")
                ->setParameter('search', '%' . strtolower($search) . '%')
                ->setParameter('searchRaw', '%' . $search . '%');
        }

        $countQb = clone $baseQb;
        $totalCards = (int) $countQb
            ->select('COUNT(vc.id)')
            ->fetchOne();

        $totalPages = max(1, (int) ceil($totalCards / $perPage));
        $page = min($page, $totalPages);

        $cards = (clone $baseQb)
            ->select('vc.id', 'vc.card_number', 'vc.expiry_date', 'vc.status', 'vc.created_at', 'vc.wallet_id', 'u.full_name', 'u.email')
            ->orderBy($sortMap[$sort], $direction)
            ->addOrderBy('vc.id', 'DESC')
            ->setFirstResult(($page - 1) * $perPage)
            ->setMaxResults($perPage)
            ->fetchAllAssociative();

        return $this->render('admin/virtual_cards.html.twig', [
            'cards' => $cards,
            'stats' => [
                'total' => (int) ($stats['total_cards'] ?? 0),
                'active' => (int) ($stats['active_cards'] ?? 0),
                'frozen' => (int) ($stats['frozen_cards'] ?? 0),
                'expired' => (int) ($stats['expired_cards'] ?? 0),
            ],
            'search' => $search,
            'sort' => $sort,
            'direction' => strtolower($direction),
            'currentPage' => $page,
            'totalPages' => $totalPages,
            'totalCards' => $totalCards,
        ]);
    }

    #[Route('/admin/virtual-cards/{id}/edit', name: 'app_admin_virtual_card_edit')]
    public function edit(VirtualCard $card, Request $request, EntityManagerInterface $entityManager): Response
    {
        $form = $this->createForm(AdminVirtualCardType::class, $card);
        $form->handleRequest($request);

        if ($form->isSubmitted() && $form->isValid()) {
            $entityManager->flush();

            $this->addFlash('success', 'Virtual card updated successfully.');

            return $this->redirectToRoute('app_admin_virtual_cards');
        }

        $status = ($form->isSubmitted() && !$form->isValid()) ? Response::HTTP_UNPROCESSABLE_ENTITY : Response::HTTP_OK;
        $owner = $entityManager->getConnection()->fetchAssociative(
            'SELECT u.full_name, u.email FROM virtual_cards vc LEFT JOIN wallets w ON w.id = vc.wallet_id LEFT JOIN users_local u ON u.user_id = w.user_id WHERE vc.id = :cardId',
            ['cardId' => $card->getId()]
        ) ?: ['full_name' => null, 'email' => null];

        return $this->render('admin/virtual_card_edit.html.twig', [
            'card' => $card,
            'owner' => [
                'fullName' => $owner['full_name'] ?: null,
                'email' => $owner['email'] ?: null,
            ],
            'form' => $form->createView(),
        ], new Response(null, $status));
    }
}
