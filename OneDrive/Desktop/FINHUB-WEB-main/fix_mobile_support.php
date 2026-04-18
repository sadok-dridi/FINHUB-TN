<?php
$file = '/home/sadok/Documents/projects/PIDEV/FINHUB-WEB/templates/support/index.html.twig';
$content = file_get_contents($file);

$css_to_add = "
        /* --- MOBILE RESPONSIVENESS --- */
        @media (max-width: 768px) {
            .support-wrapper {
                height: calc(100vh - 80px); /* Adjust for mobile header */
                gap: 1rem;
            }
            .support-title {
                font-size: 1.4rem;
            }
            .support-subtitle {
                font-size: 0.9rem;
            }
            .support-tabs {
                overflow-x: auto;
                white-space: nowrap;
                padding: 0.5rem;
                -ms-overflow-style: none; /* IE and Edge */
                scrollbar-width: none; /* Firefox */
            }
            .support-tabs::-webkit-scrollbar {
                display: none;
            }
            .support-tab {
                padding: 0.6rem 1rem;
                font-size: 0.85rem;
                flex-shrink: 0;
            }
            .chat-messages {
                padding: 1rem;
            }
            .msg-bubble {
                max-width: 90%;
                padding: 0.75rem 1rem;
                font-size: 0.9rem;
            }
            .chat-input-wrapper {
                padding: 0.75rem 1rem;
                gap: 0.5rem;
            }
            .chat-input {
                padding: 0.75rem 1rem;
            }
            .btn-send {
                padding: 0.75rem 1rem;
            }
            .ticket-card {
                flex-direction: column;
                align-items: flex-start;
                gap: 1rem;
            }
            .ticket-title-row {
                flex-wrap: wrap;
                gap: 0.5rem;
            }
            .alerts-title, .tickets-title {
                font-size: 1.2rem;
            }
            .kb-search-wrapper {
                flex-direction: column;
                width: 100%;
            }
            .kb-search-input {
                width: 100%;
            }
            .btn-kb-search {
                width: 100%;
            }
            .modal-content {
                width: 95%;
                padding: 1.5rem;
                margin: 1rem;
            }
            .alert-card {
                padding: 1rem;
                flex-direction: column;
            }
            .alert-icon {
                margin-bottom: 0.5rem;
            }
            .t-msg-bubble {
                max-width: 95%;
                padding: 1rem;
            }
        }
    </style>";

$content = str_replace("    </style>", $css_to_add, $content);
file_put_contents($file, $content);
echo "Added mobile responsiveness.\n";
