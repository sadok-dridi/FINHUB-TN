<?php
$file = '/home/sadok/Documents/projects/PIDEV/FINHUB-WEB/templates/support/index.html.twig';
$content = file_get_contents($file);

// Replace CSS
$css_search = '.alert-card::before { content: ""; position: absolute; left: 0; top: 10%; bottom: 10%; width: 4px; background: #f59e0b; border-radius: 0 4px 4px 0; }
        .alert-icon { color: #f59e0b; flex-shrink: 0; margin-top: 2px; }';
        
$css_replace = '.alert-card::before { content: ""; position: absolute; left: 0; top: 10%; bottom: 10%; width: 4px; border-radius: 0 4px 4px 0; }
        .alert-icon { flex-shrink: 0; margin-top: 2px; }
        .alert-warning::before { background: #f59e0b; }
        .alert-critical::before { background: #ef4444; }
        .alert-info::before { background: #a855f7; }';

$content = str_replace($css_search, $css_replace, $content);

// Replace HTML
$html_search = '<div class="alert-card">
                    <div class="alert-icon">
                        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"></path><line x1="12" y1="9" x2="12" y2="13"></line><line x1="12" y1="17" x2="12.01" y2="17"></line></svg>
                    </div>
                    <div class="alert-content">
                        <div class="alert-message">{{ alert.message }}</div>
                        <div class="alert-meta">{{ alert.source }} • {{ alert.created_at|date("M d, H:i") }}</div>
                    </div>
                </div>';

$html_replace = '{% set sev = alert.severity|upper %}
                {% set card_class = "alert-warning" %}
                {% if sev == "CRITICAL" or sev == "ERROR" %}
                    {% set card_class = "alert-critical" %}
                {% elseif sev == "INFO" %}
                    {% set card_class = "alert-info" %}
                {% endif %}

                <div class="alert-card {{ card_class }}">
                    <div class="alert-icon">
                        {% if sev == "CRITICAL" or sev == "ERROR" %}
                            <svg width="24" height="24" viewBox="0 0 24 24" fill="#ef4444" stroke="#ef4444" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"></circle><line x1="15" y1="9" x2="9" y2="15" stroke="#1e1b2e"></line><line x1="9" y1="9" x2="15" y2="15" stroke="#1e1b2e"></line></svg>
                        {% elseif sev == "INFO" %}
                            <svg width="24" height="24" viewBox="0 0 24 24" fill="#a855f7" stroke="#a855f7" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"></circle><line x1="12" y1="16" x2="12" y2="12" stroke="#1e1b2e"></line><line x1="12" y1="8" x2="12.01" y2="8" stroke="#1e1b2e"></line></svg>
                        {% else %}
                            <svg width="24" height="24" viewBox="0 0 24 24" fill="#f59e0b" stroke="#f59e0b" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"></path><line x1="12" y1="9" x2="12" y2="13" stroke="#1e1b2e"></line><line x1="12" y1="17" x2="12.01" y2="17" stroke="#1e1b2e"></line></svg>
                        {% endif %}
                    </div>
                    <div class="alert-content">
                        <div class="alert-message">{{ alert.message }}</div>
                        <div class="alert-meta">{{ alert.source }} • {{ alert.created_at|date("M d, H:i") }}</div>
                    </div>
                </div>';

$content = str_replace($html_search, $html_replace, $content);
file_put_contents($file, $content);
echo "Done\n";
