<?php
require 'vendor/autoload.php';
$kernel = new App\Kernel('dev', true);
$kernel->boot();
$conn = $kernel->getContainer()->get('database_connection');
$stmt = $conn->executeQuery('DESCRIBE blockchain_ledger');
print_r($stmt->fetchAllAssociative());
