<?php
require 'vendor/autoload.php';
$kernel = new App\Kernel('dev', true);
$kernel->boot();
$conn = $kernel->getContainer()->get('database_connection');
$dbTime = $conn->fetchOne('SELECT NOW()');
$phpTime = (new \DateTime())->format('Y-m-d H:i:s');
echo "DB Time:  $dbTime\nPHP Time: $phpTime\n";
