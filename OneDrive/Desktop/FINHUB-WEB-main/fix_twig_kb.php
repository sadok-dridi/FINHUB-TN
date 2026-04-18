<?php
$file = '/home/sadok/Documents/projects/PIDEV/FINHUB-WEB/templates/support/index.html.twig';
$content = file_get_contents($file);

// 1. Add Knowledge Base Tab Button
$search_tab = '<button class="support-tab" data-target="pane-alerts">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"></path><path d="M13.73 21a2 2 0 0 1-3.46 0"></path></svg>
            System Alerts
        </button>';

$replace_tab = $search_tab . '
        <button class="support-tab" data-target="pane-kb">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"></circle><path d="M9.09 9a3 3 0 0 1 5.83 1c0 2-3 3-3 3"></path><line x1="12" y1="17" x2="12.01" y2="17"></line></svg>
            Knowledge Base
        </button>';

$content = str_replace($search_tab, $replace_tab, $content);

// 2. Add Knowledge Base CSS
$search_css = '/* --- ALERTS CSS --- */';
$replace_css = '/* --- KNOWLEDGE BASE CSS --- */
        .kb-container { display: flex; flex-direction: column; gap: 1.5rem; height: 100%; }
        .kb-search-wrapper { display: flex; gap: 1rem; align-items: center; margin-bottom: 1rem; }
        .kb-search-input { flex: 1; background: rgba(255, 255, 255, 0.05); border: 1px solid rgba(255, 255, 255, 0.1); color: #fff; padding: 0.875rem 1.25rem; border-radius: 20px; outline: none; font-size: 1rem; }
        .kb-search-input:focus { border-color: #a78bfa; background: rgba(255, 255, 255, 0.08); }
        .btn-kb-search { background: linear-gradient(135deg, #a855f7, #d946ef); color: white; border: none; padding: 0.875rem 2rem; border-radius: 20px; font-weight: 600; cursor: pointer; transition: all 0.2s ease; }
        .btn-kb-search:hover { opacity: 0.9; transform: translateY(-1px); }
        
        .kb-list { display: flex; flex-direction: column; gap: 1rem; overflow-y: auto; padding-right: 0.5rem; flex: 1; }
        .kb-card { background: var(--card-bg, #1e1b2e); border: 1px solid rgba(255, 255, 255, 0.05); border-radius: 12px; padding: 1.5rem; cursor: pointer; transition: all 0.2s ease; display: flex; flex-direction: column; gap: 0.75rem; }
        .kb-card:hover { border-color: rgba(167, 139, 250, 0.4); background: rgba(255,255,255,0.02); transform: translateY(-2px); }
        .kb-header-row { display: flex; align-items: center; gap: 0.75rem; }
        .kb-tag { border: 1px solid #64748b; color: #94a3b8; font-size: 0.75rem; padding: 0.1rem 0.5rem; border-radius: 4px; text-transform: uppercase; }
        .kb-title { font-size: 1.15rem; font-weight: 600; color: #a78bfa; }
        .kb-content { font-size: 0.95rem; color: #e2e8f0; line-height: 1.6; display: -webkit-box; -webkit-line-clamp: 3; -webkit-box-orient: vertical; overflow: hidden; text-overflow: ellipsis; }
        .kb-content.expanded { -webkit-line-clamp: unset; overflow: visible; }
        
        ' . $search_css;

$content = str_replace($search_css, $replace_css, $content);

// 3. Add Knowledge Base HTML Pane
$search_html = '<!-- TICKET DETAIL PANE -->';
$replace_html = '<!-- KNOWLEDGE BASE PANE -->
    <div class="tab-pane" id="pane-kb">
        <div class="kb-container">
            <div class="kb-search-wrapper">
                <input type="text" id="kb-search-input" class="kb-search-input" placeholder="Search knowledge base...">
                <button class="btn-kb-search" id="btn-kb-search">Search</button>
            </div>
            
            <div class="kb-list" id="kb-list">
                {% for kb in kbArticles %}
                <div class="kb-card" onclick="this.querySelector(\'.kb-content\').classList.toggle(\'expanded\')">
                    <div class="kb-header-row">
                        <span class="kb-tag">{{ kb.category }}</span>
                        <span class="kb-title">{{ kb.title }}</span>
                    </div>
                    <div class="kb-content">{{ kb.content }}</div>
                </div>
                {% else %}
                <div style="color: #94a3b8; text-align: center; padding: 2rem;">No knowledge base articles found.</div>
                {% endfor %}
            </div>
        </div>
    </div>

    ' . $search_html;

$content = str_replace($search_html, $replace_html, $content);

// 4. Add JS search logic for KB
$search_js = '// --- Tickets Logic ---';
$replace_js = '// --- Knowledge Base Search Logic ---
    const kbSearchInput = document.getElementById(\'kb-search-input\');
    const btnKbSearch = document.getElementById(\'btn-kb-search\');
    const kbCards = document.querySelectorAll(\'.kb-card\');

    function performKbSearch() {
        const query = kbSearchInput.value.toLowerCase().trim();
        kbCards.forEach(card => {
            const title = card.querySelector(\'.kb-title\').textContent.toLowerCase();
            const content = card.querySelector(\'.kb-content\').textContent.toLowerCase();
            const tag = card.querySelector(\'.kb-tag\').textContent.toLowerCase();
            
            if (query === \'\' || title.includes(query) || content.includes(query) || tag.includes(query)) {
                card.style.display = \'flex\';
            } else {
                card.style.display = \'none\';
            }
        });
    }

    btnKbSearch.addEventListener(\'click\', performKbSearch);
    kbSearchInput.addEventListener(\'keyup\', performKbSearch);

    ' . $search_js;

$content = str_replace($search_js, $replace_js, $content);

file_put_contents($file, $content);
echo "Done\n";
