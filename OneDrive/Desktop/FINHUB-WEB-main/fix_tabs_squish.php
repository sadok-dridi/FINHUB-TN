<?php
$file = '/home/sadok/Documents/projects/PIDEV/FINHUB-WEB/templates/support/index.html.twig';
$content = file_get_contents($file);

// Find the mobile CSS block and fix the tabs container to prevent any shrinking
$css_search = "            .support-tabs {
                position: relative;
                z-index: 10;
                -webkit-overflow-scrolling: touch;
                overflow-x: auto;
                display: flex;
                flex-wrap: nowrap;
                padding: 0.5rem;
                -ms-overflow-style: none; /* IE and Edge */
                scrollbar-width: none; /* Firefox */
                justify-content: flex-start;
                width: 100%;
            }
            .support-tabs::-webkit-scrollbar {
                display: none;
            }
            .support-tab {
                padding: 0.75rem 1rem;
                font-size: 0.9rem;
                flex: 0 0 auto;
                width: max-content;
            }";

$css_replace = "            .support-tabs {
                position: relative;
                z-index: 10;
                -webkit-overflow-scrolling: touch;
                overflow-x: auto;
                display: flex;
                flex-wrap: nowrap;
                padding: 0.5rem;
                -ms-overflow-style: none;
                scrollbar-width: none;
                justify-content: flex-start;
                min-height: 55px; /* Force minimum height to prevent squishing vertically */
                width: 100%;
            }
            .support-tabs::-webkit-scrollbar {
                display: none;
            }
            .support-tab {
                padding: 0.5rem 1rem;
                font-size: 0.9rem;
                flex-shrink: 0;
                white-space: nowrap;
                height: 40px; /* Fixed height so they don't collapse */
            }";

$content = str_replace($css_search, $css_replace, $content);
file_put_contents($file, $content);
echo "Done\n";
