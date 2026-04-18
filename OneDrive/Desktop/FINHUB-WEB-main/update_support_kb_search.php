<?php
$file = '/home/sadok/Documents/projects/PIDEV/FINHUB-WEB/src/Controller/SupportController.php';
$content = file_get_contents($file);

// Add the HttpClientInterface use statement
$use_statement = "use Symfony\Component\HttpFoundation\JsonResponse;\nuse Symfony\Contracts\HttpClient\HttpClientInterface;";
$content = str_replace("use Symfony\Component\HttpFoundation\JsonResponse;", $use_statement, $content);

// Add the api_kb_search endpoint before the last closing brace
$endpoint = "
    #[Route('/api/kb/search', name: 'api_kb_search', methods: ['POST'])]
    public function searchKb(Request \$request, EntityManagerInterface \$em, HttpClientInterface \$httpClient): JsonResponse
    {
        \$data = json_decode(\$request->getContent(), true);
        \$query = trim(\$data['query'] ?? '');

        \$conn = \$em->getConnection();
        
        if (empty(\$query)) {
            // Return all local
            \$sql = 'SELECT * FROM knowledge_base ORDER BY created_at DESC';
            \$localResults = \$conn->executeQuery(\$sql)->fetchAllAssociative();
            return new JsonResponse(['local' => \$localResults, 'web' => []]);
        }

        // 1. Local Search (Top 1) like Java app
        \$sql = 'SELECT * FROM knowledge_base WHERE title LIKE ? OR content LIKE ? ORDER BY created_at DESC LIMIT 1';
        \$likeQuery = '%' . \$query . '%';
        \$localResults = \$conn->executeQuery(\$sql, [\$likeQuery, \$likeQuery])->fetchAllAssociative();

        // 2. Web Search via n8n webhook
        \$webResults = [];
        try {
            \$webhookUrl = 'https://crashinburn.work.gd/webhook/kb-search?q=' . urlencode(\$query);
            \$response = \$httpClient->request('GET', \$webhookUrl, [
                'timeout' => 15,
            ]);
            
            if (\$response->getStatusCode() >= 200 && \$response->getStatusCode() < 300) {
                \$content = \$response->toArray(false);
                if (is_array(\$content)) {
                    foreach (\$content as \$item) {
                        \$webResults[] = [
                            'title' => \$item['title'] ?? 'Untitled Web Result',
                            'content' => \$item['snippet'] ?? '',
                            'source' => \$item['source'] ?? 'Web Search',
                            'category' => '🌐 Web Result',
                        ];
                    }
                }
            }
        } catch (\Exception \$e) {
            // If web search fails, we continue and return just local
            \$webResults[] = ['error' => true, 'message' => \$e->getMessage()];
        }

        return new JsonResponse([
            'local' => \$localResults,
            'web' => \$webResults
        ]);
    }
}
";

// replace the last brace with the new method and a brace
$content = preg_replace('/}\s*$/', $endpoint, $content);
file_put_contents($file, $content);
echo "Controller updated.\n";
