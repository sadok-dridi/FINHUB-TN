<?php
require 'vendor/autoload.php';
(new \Symfony\Component\Dotenv\Dotenv())->bootEnv('.env');
$kernel = new \App\Kernel('dev', true);
$kernel->boot();
$request = \Symfony\Component\HttpFoundation\Request::create('/register', 'GET');
$response = $kernel->handle($request);
echo "Status Code: " . $response->getStatusCode() . "\n";
