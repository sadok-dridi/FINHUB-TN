<?php
$file = '/home/sadok/Documents/projects/PIDEV/FINHUB-WEB/src/Controller/SupportController.php';
$content = file_get_contents($file);

$search = "
        // Fetch System Alerts
        \$conn = \$em->getConnection();
        \$sql = 'SELECT * FROM system_alerts WHERE user_id = :userId ORDER BY created_at DESC';
        \$stmt = \$conn->prepare(\$sql);
        \$result = \$stmt->executeQuery(['userId' => \$user->getId()]);
        \$alerts = \$result->fetchAllAssociative();";

$replace = "
        // Fetch System Alerts
        \$conn = \$em->getConnection();
        \$sql = 'SELECT * FROM system_alerts WHERE user_id = ? ORDER BY created_at DESC';
        \$result = \$conn->executeQuery(\$sql, [\$user->getId()]);
        \$alerts = \$result->fetchAllAssociative();";

$content = str_replace($search, $replace, $content);
file_put_contents($file, $content);
echo "Done\n";
