<?php

namespace App\Controller\Admin;

use App\Form\KnowledgeBaseType;
use App\Entity\SystemAlert;
use App\Entity\User;
use App\Entity\SupportMessage;
use App\Entity\SupportTicket;
use App\Repository\SupportTicketRepository;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\Form\FormFactoryInterface;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Annotation\Route;
use Symfony\Component\Security\Http\Attribute\IsGranted;

#[IsGranted('ROLE_ADMIN')]
class SupportController extends AbstractController
{
    #[Route('/admin/support', name: 'app_admin_support')]
    public function index(SupportTicketRepository $ticketRepository): Response
    {
        $tickets = $ticketRepository->findBy([], ['created_at' => 'DESC']);

        return $this->render('admin/support.html.twig', [
            'tickets' => $tickets,
        ]);
    }

    #[Route('/admin/support/alerts', name: 'app_admin_support_alerts', methods: ['GET'])]
    public function alerts(EntityManagerInterface $em): Response
    {
        // Get all broadcasts (grouped by message and created_at, same as Java)
        $conn = $em->getConnection();
        $sql = "SELECT MIN(id) as id, severity, message, source, created_at 
                FROM system_alerts 
                GROUP BY severity, message, source, created_at 
                ORDER BY created_at DESC";
        $stmt = $conn->prepare($sql);
        $resultSet = $stmt->executeQuery();
        $broadcasts = $resultSet->fetchAllAssociative();

        return $this->render('admin/support_alerts.html.twig', [
            'broadcasts' => $broadcasts,
        ]);
    }

    #[Route('/admin/support/alerts/broadcast', name: 'app_admin_support_alerts_broadcast', methods: ['POST'])]
    public function broadcastAlert(Request $request, EntityManagerInterface $em, \Symfony\Component\Form\FormFactoryInterface $formFactory): JsonResponse
    {
        $data = json_decode($request->getContent(), true);
        
        $form = $formFactory->createNamed('', \App\Form\SystemAlertType::class);
        $form->submit($data);

        if (!$form->isValid()) {
            $errors = [];
            foreach ($form->getErrors(true) as $error) {
                $path = '';
                $parent = $error->getOrigin();
                while ($parent && $parent->getName() !== '') {
                    if ($path === '') {
                        $path = $parent->getName();
                    } else {
                        $path = $parent->getName() . '.' . $path;
                    }
                    $parent = $parent->getParent();
                }
                $errors[$path] = $error->getMessage();
            }
            return new JsonResponse(['success' => false, 'errors' => $errors]);
        }

        $severity = $form->get('severity')->getData();
        $source = trim($form->get('source')->getData());
        $messageText = trim($form->get('message')->getData());

        $users = $em->getRepository(User::class)->findAll();
        $createdAt = new \DateTime('now', new \DateTimeZone('Africa/Tunis')); // use same time for all to allow grouping

        foreach ($users as $user) {
            $alert = new SystemAlert();
            $alert->setUser($user);
            $alert->setSeverity($severity);
            $alert->setSource($source);
            $alert->setMessage($messageText);
            $alert->setCreatedAt($createdAt);
            
            $em->persist($alert);
        }

        $em->flush();

        return new JsonResponse(['success' => true, 'message' => 'Alert broadcasted successfully.']);
    }

    #[Route('/admin/support/alerts/delete', name: 'app_admin_support_alerts_delete', methods: ['POST'])]
    public function deleteBroadcast(Request $request, EntityManagerInterface $em): JsonResponse
    {
        $data = json_decode($request->getContent(), true);
        
        $message = $data['message'] ?? '';
        $createdAt = $data['created_at'] ?? '';

        if (empty($message) || empty($createdAt)) {
            return new JsonResponse(['success' => false, 'message' => 'Invalid broadcast data.']);
        }

        $conn = $em->getConnection();
        $sql = "DELETE FROM system_alerts WHERE message = :msg AND created_at = :cat";
        $conn->executeStatement($sql, ['msg' => $message, 'cat' => $createdAt]);

        return new JsonResponse(['success' => true, 'message' => 'Broadcast deleted for all users.']);
    }

    #[Route('/admin/support/kyc', name: 'app_admin_support_kyc', methods: ['GET'])]
    public function kyc(EntityManagerInterface $em): Response
    {
        $conn = $em->getConnection();
        $sql = "SELECT k.*, u.email, u.full_name, u.role, u.phone_number, u.trust_score, u.profile_photo_url
                FROM kyc_requests k
                JOIN users_local u ON k.user_id = u.user_id
                ORDER BY CASE WHEN k.status = 'PENDING' THEN 1 ELSE 2 END, k.submission_date DESC";
        $stmt = $conn->prepare($sql);
        $resultSet = $stmt->executeQuery();
        $kycRequests = $resultSet->fetchAllAssociative();

        return $this->render('admin/support_kyc.html.twig', [
            'kyc_requests' => $kycRequests,
        ]);
    }

    #[Route('/admin/support/kyc/{id}/status', name: 'app_admin_support_kyc_status', methods: ['POST'])]
    public function updateKycStatus(int $id, Request $request, EntityManagerInterface $em): JsonResponse
    {
        $data = json_decode($request->getContent(), true);
        $status = $data['status'] ?? '';

        if (!in_array($status, ['APPROVED', 'REJECTED'])) {
            return new JsonResponse(['success' => false, 'message' => 'Invalid status.'], 400);
        }

        $conn = $em->getConnection();
        $sql = "UPDATE kyc_requests SET status = :status WHERE request_id = :id";
        $conn->executeStatement($sql, ['status' => $status, 'id' => $id]);

        return new JsonResponse(['success' => true, 'message' => 'KYC request status updated.']);
    }

    #[Route('/admin/support/kb', name: 'app_admin_support_kb', methods: ['GET'])]
    public function kb(Request $request, EntityManagerInterface $em, FormFactoryInterface $formFactory): Response
    {
        $search = $request->query->get('q', '');
        $conn = $em->getConnection();
        $form = $formFactory->createNamed('', KnowledgeBaseType::class, null, [
            'action' => $this->generateUrl('app_admin_support_kb_save'),
            'method' => 'POST',
            'attr' => [
                'id' => 'kbArticleForm',
                'novalidate' => 'novalidate',
            ],
        ]);

        if (!empty($search)) {
            $sql = "SELECT * FROM knowledge_base WHERE title LIKE :search OR content LIKE :search ORDER BY created_at DESC";
            $resultSet = $conn->executeQuery($sql, ['search' => '%' . $search . '%']);
        } else {
            $sql = "SELECT * FROM knowledge_base ORDER BY created_at DESC";
            $resultSet = $conn->executeQuery($sql);
        }
        
        $articles = $resultSet->fetchAllAssociative();

        return $this->render('admin/support_kb.html.twig', [
            'articles' => $articles,
            'searchQuery' => $search,
            'kbForm' => $form->createView(),
        ]);
    }

    #[Route('/admin/support/kb/save', name: 'app_admin_support_kb_save', methods: ['POST'])]
    public function kbSave(Request $request, EntityManagerInterface $em, FormFactoryInterface $formFactory): JsonResponse
    {
        $id = trim((string) $request->request->get('article_id', ''));

        $form = $formFactory->createNamed('', KnowledgeBaseType::class);
        $form->handleRequest($request);

        if (!$form->isSubmitted()) {
            return new JsonResponse(['success' => false, 'message' => 'The article form was not submitted correctly.'], 400);
        }

        if (!$form->isValid()) {
            $errors = [];
            foreach ($form->getErrors(true) as $error) {
                $path = '';
                $parent = $error->getOrigin();
                while ($parent && $parent->getName() !== '') {
                    if ($path === '') {
                        $path = $parent->getName();
                    } else {
                        $path = $parent->getName() . '.' . $path;
                    }
                    $parent = $parent->getParent();
                }
                $errors[$path] = $error->getMessage();
            }
            return new JsonResponse(['success' => false, 'errors' => $errors]);
        }

        $title = trim($form->get('title')->getData());
        $category = trim($form->get('category')->getData());
        $content = trim($form->get('content')->getData());

        $conn = $em->getConnection();
        
        if ($id) {
            $sql = "UPDATE knowledge_base SET title = :title, category = :category, content = :content WHERE id = :id";
            $conn->executeStatement($sql, [
                'title' => $title,
                'category' => $category,
                'content' => $content,
                'id' => $id
            ]);
            $msg = 'Article updated successfully.';
        } else {
            $sql = "INSERT INTO knowledge_base (title, category, content) VALUES (:title, :category, :content)";
            $conn->executeStatement($sql, [
                'title' => $title,
                'category' => $category,
                'content' => $content
            ]);
            $msg = 'Article created successfully.';
        }

        return new JsonResponse(['success' => true, 'message' => $msg]);
    }

    #[Route('/admin/support/kb/delete/{id}', name: 'app_admin_support_kb_delete', methods: ['DELETE'])]
    public function kbDelete(int $id, EntityManagerInterface $em): JsonResponse
    {
        $conn = $em->getConnection();
        $sql = "DELETE FROM knowledge_base WHERE id = :id";
        $conn->executeStatement($sql, ['id' => $id]);

        return new JsonResponse(['success' => true, 'message' => 'Article deleted successfully.']);
    }

    #[Route('/admin/support/{id}', name: 'app_admin_support_details', methods: ['GET'])]
    public function details(SupportTicket $ticket): Response
    {
        return $this->render('admin/support_details.html.twig', [
            'ticket' => $ticket,
            'messages' => $ticket->getMessages()
        ]);
    }

    #[Route('/admin/support/{id}/reply', name: 'app_admin_support_reply', methods: ['POST'])]
    public function reply(Request $request, SupportTicket $ticket, EntityManagerInterface $em): JsonResponse
    {
        $data = json_decode($request->getContent(), true);
        $messageText = $data['message'] ?? '';

        if (empty(trim($messageText))) {
            return new JsonResponse(['success' => false, 'message' => 'Message cannot be empty.']);
        }

        if (in_array($ticket->getStatus(), ['CLOSED', 'RESOLVED'])) {
            return new JsonResponse(['success' => false, 'message' => 'Cannot reply to a closed or resolved ticket.']);
        }

        $message = new SupportMessage();
        $message->setTicket($ticket);
        $message->setSenderRole('ADMIN');
        $message->setMessage($messageText);
        $message->setCreatedAt(new \DateTime('now', new \DateTimeZone('Africa/Tunis')));

        $em->persist($message);
        $em->flush();

        return new JsonResponse(['success' => true, 'message' => 'Reply sent successfully.']);
    }

    #[Route('/admin/support/{id}/resolve', name: 'app_admin_support_resolve', methods: ['POST'])]
    public function resolve(SupportTicket $ticket, EntityManagerInterface $em): JsonResponse
    {
        if (in_array($ticket->getStatus(), ['CLOSED', 'RESOLVED'])) {
            return new JsonResponse(['success' => false, 'message' => 'Ticket is already closed or resolved.']);
        }

        $ticket->setStatus('RESOLVED');
        $ticket->setResolvedAt(new \DateTime('now', new \DateTimeZone('Africa/Tunis')));
        $em->flush();

        return new JsonResponse(['success' => true, 'message' => 'Ticket marked as resolved.']);
    }

}
