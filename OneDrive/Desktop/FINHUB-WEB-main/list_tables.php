<?php
require 'vendor/autoload.php';
$kernel = new App\Kernel('dev', true);
$kernel->boot();
$container = $kernel->getContainer();
$em = $container->get('doctrine')->getManager();
$conn = $em->getConnection();
$tables = $conn->createSchemaManager()->listTableNames();
print_r($tables);
