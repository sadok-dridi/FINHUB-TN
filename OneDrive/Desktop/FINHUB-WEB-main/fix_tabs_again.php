<?php
$file = '/home/sadok/Documents/projects/PIDEV/FINHUB-WEB/templates/support/index.html.twig';
$content = file_get_contents($file);

// Replace the complicated JS back to simple, working clicks
$js_search = "        const switchTab = (e) => {
            e.preventDefault();
            tabs.forEach(t => t.classList.remove('active'));
            panes.forEach(p => p.classList.remove('active'));
            tab.classList.add('active');
            document.getElementById(tab.dataset.target).classList.add('active');
        };
        tab.addEventListener('click', switchTab);
        tab.addEventListener('touchend', (e) => {
            // Prevent simulated click from firing right after touchend
            e.preventDefault();
            switchTab(e);
        });";

$js_replace = "        tab.addEventListener('click', (e) => {
            e.preventDefault();
            tabs.forEach(t => t.classList.remove('active'));
            panes.forEach(p => p.classList.remove('active'));
            tab.classList.add('active');
            document.getElementById(tab.dataset.target).classList.add('active');
        });";
$content = str_replace($js_search, $js_replace, $content);

// Ensure the mobile CSS for support-tabs forces them to keep their size
$css_search = "        @media (max-width: 768px) {
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
            position: relative;
            z-index: 10;
            -webkit-overflow-scrolling: touch;
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
            }";

$css_replace = "        @media (max-width: 768px) {
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
$content = str_replace($css_search, $css_replace, $content);

file_put_contents($file, $content);
echo "Done\n";
