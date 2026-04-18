<?php
$file = '/home/sadok/Documents/projects/PIDEV/FINHUB-WEB/templates/support/index.html.twig';
$content = file_get_contents($file);

$js_search = "    let originalKbHtml = kbList.innerHTML;";
$js_replace = "    let originalKbHtml = kbList.innerHTML;

    window.openWebResult = function(source) {
        if (!source) return;
        const index = source.indexOf('http');
        if (index !== -1) {
            window.open(source.substring(index).trim(), '_blank');
        }
    };";
$content = str_replace($js_search, $js_replace, $content);

$html_search = 'onclick="window.open(\'${web.source}\', \'_blank\')"';
$html_replace = 'onclick="window.openWebResult(\'${web.source}\')"';
$content = str_replace($html_search, $html_replace, $content);

file_put_contents($file, $content);
echo "Updated web link logic.\n";
