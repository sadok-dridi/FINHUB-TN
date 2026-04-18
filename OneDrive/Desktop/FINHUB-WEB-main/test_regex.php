<?php
$refs = [
    "Transfer from sadok (Wallet 27)",
    "Transfer to seji (Wallet 29)",
    "Plaid External Bank Transfer",
    "Escrow Released to ilef",
];

foreach ($refs as $ref) {
    if (preg_match('/(?:to|from) (.+?)(?: \(|$)/i', $ref, $matches)) {
        echo "Match for '$ref': " . $matches[1] . "\n";
    } else {
        echo "No match for '$ref'\n";
    }
}
