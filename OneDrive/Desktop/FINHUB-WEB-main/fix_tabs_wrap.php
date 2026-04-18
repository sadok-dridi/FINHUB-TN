<?php
$file = '/home/sadok/Documents/projects/PIDEV/FINHUB-WEB/templates/support/index.html.twig';
$content = file_get_contents($file);

$css_search = "            .support-tabs {
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

$css_replace = "            .support-tabs {
                position: relative;
                z-index: 10;
                display: flex;
                flex-wrap: wrap; /* Turn off horizontal sliding and wrap to next line */
                gap: 0.5rem;
                padding: 0.5rem;
                justify-content: center;
                width: 100%;
                height: auto;
                overflow-x: visible;
            }
            .support-tab {
                padding: 0.5rem;
                font-size: 0.85rem;
                flex: 1 1 calc(50% - 0.5rem); /* Creates a neat 2x2 grid on mobile */
                white-space: nowrap;
                height: 40px;
                display: flex;
                justify-content: center;
                align-items: center;
            }";

$content = str_replace($css_search, $css_replace, $content);
file_put_contents($file, $content);
echo "Done\n";
