<?php
require __DIR__.'/vendor/autoload.php';
use Symfony\Component\Dotenv\Dotenv;
(new Dotenv())->bootEnv(__DIR__.'/.env');

$kernel = new App\Kernel('dev', true);
$kernel->boot();
$container = $kernel->getContainer();
$em = $container->get('doctrine.orm.entity_manager');
$conn = $em->getConnection();

$id = 1;
$status = 'APPROVED';
$sql = "UPDATE kyc_requests SET status = :status WHERE request_id = :id";
try {
    $conn->executeStatement($sql, ['status' => $status, 'id' => $id]);
    echo "Success!\n";
} catch (\Exception $e) {
    echo "Error: " . $e->getMessage() . "\n";
}
