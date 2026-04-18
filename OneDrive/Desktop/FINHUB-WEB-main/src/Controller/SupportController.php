<?php

namespace App\Controller;

use App\Entity\SupportTicket;
use App\Entity\SupportMessage;
use App\Repository\SupportTicketRepository;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Annotation\Route;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Contracts\HttpClient\HttpClientInterface;
use App\Service\ChatAssistantService;

class SupportController extends AbstractController
{
    #[Route('/support', name: 'app_support')]
    public function index(SupportTicketRepository $ticketRepo, EntityManagerInterface $em): Response
    {
        $user = $this->getUser();
        if (!$user) {
            return $this->redirectToRoute('app_login');
        }

        // Fetch user tickets ordered by latest
        $tickets = $ticketRepo->findBy(['user' => $user], ['created_at' => 'DESC']);

        
        // Fetch System Alerts
        $conn = $em->getConnection();
        $sql = 'SELECT * FROM system_alerts WHERE user_id = ? ORDER BY created_at DESC';
        $result = $conn->executeQuery($sql, [$user->getId()]);
        $alerts = $result->fetchAllAssociative();

        // Fetch Knowledge Base Articles
        $sqlKb = 'SELECT * FROM knowledge_base ORDER BY created_at DESC';
        $resultKb = $conn->executeQuery($sqlKb);
        $kbArticles = $resultKb->fetchAllAssociative();

        return $this->render('support/index.html.twig', [
            'tickets' => $tickets,
            'alerts' => $alerts,
            'kbArticles' => $kbArticles
        ]);
    }

    #[Route('/api/chat', name: 'api_chat', methods: ['POST'])]
    public function chat(Request $request, ChatAssistantService $chatService): JsonResponse
    {
        $data = json_decode($request->getContent(), true);
        $message = $data['message'] ?? '';

        if (empty(trim($message))) {
             return new JsonResponse(['response' => 'How can I help you today?']);
        }

        $response = $chatService->getResponse($message);

        return new JsonResponse(['response' => $response]);
    }

    #[Route('/api/ticket/create', name: 'api_ticket_create', methods: ['POST'])]
    public function createTicket(Request $request, EntityManagerInterface $em, \Symfony\Component\Form\FormFactoryInterface $formFactory): JsonResponse
    {
        $user = $this->getUser();
        if (!$user) {
            return new JsonResponse(['error' => 'Unauthorized'], 401);
        }

        $data = json_decode($request->getContent(), true);

        $form = $formFactory->createNamed('', \App\Form\TicketType::class);
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

        $subject = $form->get('subject')->getData();
        $category = $form->get('category')->getData();
        $messageContent = $form->get('message')->getData();

        $ticket = new SupportTicket();
        $ticket->setUser($user);
        $ticket->setSubject($subject);
        $ticket->setCategory($category);
        $ticket->setStatus('OPEN');
        $ticket->setPriority('NORMAL');

        $message = new SupportMessage();
        $message->setSenderRole('USER');
        $message->setMessage($messageContent);
        $message->setCreatedAt(new \DateTime());
        $ticket->addMessage($message);

        $em->persist($ticket);
        $em->persist($message);
        $em->flush();

        return new JsonResponse([
            'success' => true,
            'ticket' => [
                'id' => $ticket->getId(),
                'subject' => $ticket->getSubject(),
                'category' => $ticket->getCategory(),
                'status' => $ticket->getStatus(),
                'created_at' => $ticket->getCreatedAt()->format('M d, H:i')
            ]
        ]);
    }

    #[Route('/api/ticket/{id}', name: 'api_ticket_get', methods: ['GET'])]
    public function getTicket(SupportTicket $ticket): JsonResponse
    {
        if ($ticket->getUser() !== $this->getUser()) {
            return new JsonResponse(['error' => 'Unauthorized'], 403);
        }

        $messages = [];
        foreach ($ticket->getMessages() as $msg) {
            $messages[] = [
                'id' => $msg->getId(),
                'sender_role' => $msg->getSenderRole(),
                'message' => $msg->getMessage(),
                'created_at' => $msg->getCreatedAt()->format('M d, Y, H:i')
            ];
        }

        return new JsonResponse([
            'id' => $ticket->getId(),
            'subject' => $ticket->getSubject(),
            'status' => $ticket->getStatus(),
            'category' => $ticket->getCategory(),
            'messages' => $messages
        ]);
    }

    #[Route('/api/ticket/{id}/reply', name: 'api_ticket_reply', methods: ['POST'])]
    public function replyTicket(Request $request, SupportTicket $ticket, EntityManagerInterface $em): JsonResponse
    {
        if ($ticket->getUser() !== $this->getUser()) {
            return new JsonResponse(['error' => 'Unauthorized'], 403);
        }

        if (in_array($ticket->getStatus(), ['CLOSED', 'RESOLVED'], true)) {
            return new JsonResponse(['error' => 'This ticket is already closed and cannot receive new messages.'], 400);
        }

        $data = json_decode($request->getContent(), true);
        $messageContent = $data['message'] ?? '';

        if (empty($messageContent)) {
            return new JsonResponse(['error' => 'Message is required'], 400);
        }

        $message = new SupportMessage();
        $message->setSenderRole('USER');
        $message->setMessage($messageContent);
        $message->setCreatedAt(new \DateTime());
        $ticket->addMessage($message);

        $em->persist($message);
        $em->flush();

        return new JsonResponse([
            'success' => true,
            'message' => [
                'id' => $message->getId(),
                'sender_role' => $message->getSenderRole(),
                'message' => $message->getMessage(),
                'created_at' => $message->getCreatedAt()->format('M d, Y, H:i')
            ]
        ]);
    }

    #[Route('/api/kb/search', name: 'api_kb_search', methods: ['POST'])]
    public function searchKb(Request $request, EntityManagerInterface $em, HttpClientInterface $httpClient): JsonResponse
    {
        $data = json_decode($request->getContent(), true);
        $query = trim($data['query'] ?? '');

        $conn = $em->getConnection();
        
        if (empty($query)) {
            // Return all local
            $sql = 'SELECT * FROM knowledge_base ORDER BY created_at DESC';
            $localResults = $conn->executeQuery($sql)->fetchAllAssociative();
            return new JsonResponse(['local' => $localResults, 'web' => []]);
        }

        // 1. Local Search (Top 1) like Java app
        $sql = 'SELECT * FROM knowledge_base WHERE title LIKE ? OR content LIKE ? ORDER BY created_at DESC LIMIT 1';
        $likeQuery = '%' . $query . '%';
        $localResults = $conn->executeQuery($sql, [$likeQuery, $likeQuery])->fetchAllAssociative();

        // 2. Web Search via n8n webhook
        $webResults = [];
        try {
            $webhookUrl = 'https://crashinburn.work.gd/webhook/kb-search?q=' . urlencode($query);
            $response = $httpClient->request('GET', $webhookUrl, [
                'timeout' => 15,
            ]);
            
            if ($response->getStatusCode() >= 200 && $response->getStatusCode() < 300) {
                $content = $response->toArray(false);
                if (is_array($content)) {
                    foreach ($content as $item) {
                        $webResults[] = [
                            'title' => $item['title'] ?? 'Untitled Web Result',
                            'content' => $item['snippet'] ?? '',
                            'source' => $item['source'] ?? 'Web Search',
                            'category' => '🌐 Web Result',
                        ];
                    }
                }
            }
        } catch (\Exception $e) {
            // If web search fails, we continue and return just local
            $webResults[] = ['error' => true, 'message' => $e->getMessage()];
        }

        return new JsonResponse([
            'local' => $localResults,
            'web' => $webResults
        ]);
    }
}
