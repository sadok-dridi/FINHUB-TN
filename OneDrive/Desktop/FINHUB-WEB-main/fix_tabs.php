<?php
$file = '/home/sadok/Documents/projects/PIDEV/FINHUB-WEB/templates/support/index.html.twig';
$content = file_get_contents($file);

// Add type="button" to tabs to prevent any default browser action issues
$content = str_replace('<button class="support-tab', '<button type="button" class="support-tab', $content);

// Ensure CSS z-index and pointer-events just in case
$css_search = '.support-tabs {';
$css_replace = '.support-tabs {
            position: relative;
            z-index: 10;
            -webkit-overflow-scrolling: touch;';
$content = str_replace($css_search, $css_replace, $content);

// Update JS for tabs to be more robust
$js_search = "tab.addEventListener('click', () => {";
$js_replace = "tab.addEventListener('click', (e) => {
            e.preventDefault();";
$content = str_replace($js_search, $js_replace, $content);

// Also add touchstart listener just for mobile responsiveness feel
$js_search2 = "tab.addEventListener('click', (e) => {
            e.preventDefault();
            tabs.forEach(t => t.classList.remove('active'));
            panes.forEach(p => p.classList.remove('active'));
            tab.classList.add('active');
            document.getElementById(tab.dataset.target).classList.add('active');
        });";

$js_replace2 = "
        const switchTab = (e) => {
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
        
$content = str_replace($js_search2, $js_replace2, $content);

file_put_contents($file, $content);
echo "Fixed tabs for mobile.\n";
