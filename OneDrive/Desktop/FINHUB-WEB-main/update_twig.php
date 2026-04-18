<?php
$file = '/home/sadok/Documents/projects/PIDEV/FINHUB-WEB/templates/support/index.html.twig';
$content = file_get_contents($file);

// Add the Tab button
$search1 = '<button class="support-tab" data-target="pane-tickets">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z"></path></svg>
            My Tickets
        </button>';

$replace1 = $search1 . '
        <button class="support-tab" data-target="pane-alerts">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"></path><path d="M13.73 21a2 2 0 0 1-3.46 0"></path></svg>
            System Alerts
        </button>';

$content = str_replace($search1, $replace1, $content);

// Add the Tab Pane content
$search2 = '<!-- TICKET DETAIL PANE -->';

$replace2 = '<!-- SYSTEM ALERTS PANE -->
    <div class="tab-pane" id="pane-alerts">
        <div class="alerts-container">
            <div class="alerts-header">
                <div class="alerts-title">System Alerts</div>
                <button class="btn-refresh" onclick="location.reload();" title="Refresh">
                    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="23 4 23 10 17 10"></polyline><path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10"></path></svg>
                </button>
            </div>
            <div class="alerts-list">
                {% for alert in alerts %}
                <div class="alert-card">
                    <div class="alert-icon">
                        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"></path><line x1="12" y1="9" x2="12" y2="13"></line><line x1="12" y1="17" x2="12.01" y2="17"></line></svg>
                    </div>
                    <div class="alert-content">
                        <div class="alert-message">{{ alert.message }}</div>
                        <div class="alert-meta">{{ alert.source }} • {{ alert.created_at|date("M d, H:i") }}</div>
                    </div>
                </div>
                {% else %}
                <div style="color: #94a3b8; text-align: center; padding: 2rem;">No system alerts.</div>
                {% endfor %}
            </div>
        </div>
    </div>

    ' . $search2;

$content = str_replace($search2, $replace2, $content);

file_put_contents($file, $content);
echo "Done\n";
