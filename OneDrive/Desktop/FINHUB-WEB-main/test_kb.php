<?php
require __DIR__.'/vendor/autoload.php';
use Symfony\Component\Dotenv\Dotenv;
(new Dotenv())->bootEnv(__DIR__.'/.env');

$kernel = new App\Kernel('dev', true);
$kernel->boot();
$container = $kernel->getContainer();
$em = $container->get('doctrine.orm.entity_manager');
$conn = $em->getConnection();

$search = "test";
$sql = "SELECT * FROM knowledge_base WHERE title LIKE :search OR content LIKE :search ORDER BY created_at DESC";
try {
    $res = $conn->executeQuery($sql, ['search' => '%' . $search . '%'])->fetchAllAssociative();
    print_r($res);
    echo "Success!\n";
} catch (\Exception $e) {
    echo "Error: " . $e->getMessage() . "\n";
}
