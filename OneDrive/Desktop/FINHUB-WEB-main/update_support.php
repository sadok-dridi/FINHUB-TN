<?php
$file = '/home/sadok/Documents/projects/PIDEV/FINHUB-WEB/src/Controller/SupportController.php';
$content = file_get_contents($file);

$search = "return \$this->render('support/index.html.twig', [
            'tickets' => \$tickets,
        ]);";

$replace = "
        // Fetch System Alerts
        \$conn = \$em->getConnection();
        \$sql = 'SELECT * FROM system_alerts WHERE user_id = :userId ORDER BY created_at DESC';
        \$stmt = \$conn->prepare(\$sql);
        \$result = \$stmt->executeQuery(['userId' => \$user->getId()]);
        \$alerts = \$result->fetchAllAssociative();

        return \$this->render('support/index.html.twig', [
            'tickets' => \$tickets,
            'alerts' => \$alerts
        ]);";

$content = str_replace($search, $replace, $content);
file_put_contents($file, $content);
echo "Done\n";
