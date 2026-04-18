<?php
$file = '/home/sadok/Documents/projects/PIDEV/FINHUB-WEB/src/Controller/SupportController.php';
$content = file_get_contents($file);

$search = "
        // Fetch System Alerts
        \$conn = \$em->getConnection();
        \$sql = 'SELECT * FROM system_alerts WHERE user_id = ? ORDER BY created_at DESC';
        \$result = \$conn->executeQuery(\$sql, [\$user->getId()]);
        \$alerts = \$result->fetchAllAssociative();

        return \$this->render('support/index.html.twig', [
            'tickets' => \$tickets,
            'alerts' => \$alerts
        ]);";

$replace = "
        // Fetch System Alerts
        \$conn = \$em->getConnection();
        \$sql = 'SELECT * FROM system_alerts WHERE user_id = ? ORDER BY created_at DESC';
        \$result = \$conn->executeQuery(\$sql, [\$user->getId()]);
        \$alerts = \$result->fetchAllAssociative();

        // Fetch Knowledge Base Articles
        \$sqlKb = 'SELECT * FROM knowledge_base ORDER BY created_at DESC';
        \$resultKb = \$conn->executeQuery(\$sqlKb);
        \$kbArticles = \$resultKb->fetchAllAssociative();

        return \$this->render('support/index.html.twig', [
            'tickets' => \$tickets,
            'alerts' => \$alerts,
            'kbArticles' => \$kbArticles
        ]);";

$content = str_replace($search, $replace, $content);
file_put_contents($file, $content);
echo "Done\n";
