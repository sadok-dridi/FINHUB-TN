<?php
$d = new \DateTime('2024-01-01 12:00:00');
echo "If seconds are 0: " . $d->format('s') === '00' ? $d->format('Y-m-d\TH:i') : $d->format('Y-m-d\TH:i:s') . "\n";
