<?php

namespace App\Service;

use Symfony\Contracts\HttpClient\HttpClientInterface;
use Symfony\Component\DependencyInjection\ParameterBag\ParameterBagInterface;

class ChatAssistantService
{
    private $httpClient;
    private $params;

    // Toggle this to use Local LLM
    private const USE_LOCAL_LLM = true;
    private const OLLAMA_CHAT_URL = 'http://localhost:11434/api/chat';
    private const OLLAMA_MODEL = 'mistral:7b-instruct';
    private const MAX_HISTORY = 10;
    
    // In a real app, this should be in session/cache
    private $conversationHistory = [];

    public function __construct(HttpClientInterface $httpClient, ParameterBagInterface $params)
    {
        $this->httpClient = $httpClient;
        $this->params = $params;
        $this->initializeHistory();
    }

    private function initializeHistory()
    {
        $this->conversationHistory[] = [
            'role' => 'system',
            'content' => $this->getSystemPrompt()
        ];
    }

    public function getResponse(string $userMessage): string
    {
        // Add optional KB logic here later if needed
        $finalPrompt = $userMessage;

        try {
            if (self::USE_LOCAL_LLM) {
                return $this->callOllamaChatAPI($finalPrompt);
            } else {
                return $this->callGeminiAPI($finalPrompt);
            }
        } catch (\Exception $e) {
            if (self::USE_LOCAL_LLM) {
                return "I couldn't connect to your local AI (Ollama). Please ensure it's running on port 11434, or disable local mode to use the Cloud API. (Error: " . $e->getMessage() . ")";
            }
            return "I'm having trouble connecting to the cloud. Please try again later. (Error: " . $e->getMessage() . ")";
        }
    }

    private function callGeminiAPI(string $prompt): string
    {
        // Basic implementation for Gemini
        $apiKey = $this->params->get('gemini_api_key');
        $url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" . $apiKey;

        $systemContext = $this->getSystemPrompt();
        $fullPrompt = $systemContext . "\n\nUser: " . $prompt;

        $payload = [
            'contents' => [
                ['parts' => [['text' => $fullPrompt]]]
            ],
            'generationConfig' => [
                'maxOutputTokens' => 150,
                'temperature' => 0.7
            ]
        ];

        $response = $this->httpClient->request('POST', $url, [
            'json' => $payload,
            'headers' => ['Content-Type' => 'application/json']
        ]);

        if ($response->getStatusCode() === 200) {
            return $this->parseGeminiResponse($response->getContent());
        }

        return "Gemini API Error (" . $response->getStatusCode() . "): " . $response->getContent(false);
    }

    private function callOllamaChatAPI(string $userText): string
    {
        $this->conversationHistory[] = ['role' => 'user', 'content' => $userText];

        // Prune
        while (count($this->conversationHistory) > self::MAX_HISTORY) {
            array_splice($this->conversationHistory, 1, 1);
        }

        $payload = [
            'model' => self::OLLAMA_MODEL,
            'messages' => $this->conversationHistory,
            'stream' => false,
            'options' => [
                'num_ctx' => 4096,
                'temperature' => 0.7
            ]
        ];

        $response = $this->httpClient->request('POST', self::OLLAMA_CHAT_URL, [
            'json' => $payload,
            'headers' => ['Content-Type' => 'application/json'],
            'timeout' => 120
        ]);

        if ($response->getStatusCode() === 200) {
            $data = $response->toArray();
            if (isset($data['message']['content'])) {
                $assistantResponse = $data['message']['content'];
                $this->conversationHistory[] = ['role' => 'assistant', 'content' => $assistantResponse];
                return $assistantResponse;
            }
        }

        return "Ollama Error: " . $response->getStatusCode() . " - " . $response->getContent(false);
    }

    private function getSystemPrompt(): string
    {
        return "You are **FinHub Prime**, the advanced AI guardian of the FinHub Financial Ecosystem. " .
               "You are running locally on high-performance hardware (Ryzen 9 5900HX, RTX 3060), ensuring maximum privacy and speed.\n\n" .
               "**YOUR MISSION:**\n" .
               "1. **Empower Users**: Guide them through complex financial tasks (wallets, transactions, escrow) with absolute clarity.\n" .
               "2. **Guard Security**: relentlessly warn against scams, verify transaction details, and explain security features like 2FA and cold storage.\n" .
               "3. **Analyze Data**: When asked, interpret market trends or portfolio performance using your deep financial knowledge.\n\n" .
               "**YOUR PERSONALITY:**\n" .
               "- **Professional & Precise**: Use financial terminology correcty but explain it simply if asked.\n" .
               "- **Proactive**: Don't just answer; suggest the next logical step.\n" .
               "- **Concise**: Your users are busy traders. Get to the point.\n\n" .
               "**CRITICAL INSTRUCTIONS:**\n" .
               "- If the user asks about the 'Financial Twin', explain it is their AI-powered market simulation clone.\n" .
               "- If asked about 'Frozen Wallets', explain it's a security measure against suspicious activity.\n" .
               "- NEVER ask for private keys or passwords.\n" .
               "- Use Markdown formatting (bolding key terms, lists) to make your answers readable.";
    }

    private function parseGeminiResponse(string $jsonBody): string
    {
        $data = json_decode($jsonBody, true);
        if (isset($data['candidates'][0]['content']['parts'][0]['text'])) {
            return $data['candidates'][0]['content']['parts'][0]['text'];
        }
        return "I couldn't process the response.";
    }
}
