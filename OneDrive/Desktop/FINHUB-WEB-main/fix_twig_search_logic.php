<?php
$file = '/home/sadok/Documents/projects/PIDEV/FINHUB-WEB/templates/support/index.html.twig';
$content = file_get_contents($file);

$search_js = "    // --- Knowledge Base Search Logic ---
    const kbSearchInput = document.getElementById('kb-search-input');
    const btnKbSearch = document.getElementById('btn-kb-search');
    const kbCards = document.querySelectorAll('.kb-card');

    function performKbSearch() {
        const query = kbSearchInput.value.toLowerCase().trim();
        kbCards.forEach(card => {
            const title = card.querySelector('.kb-title').textContent.toLowerCase();
            const content = card.querySelector('.kb-content').textContent.toLowerCase();
            const tag = card.querySelector('.kb-tag').textContent.toLowerCase();
            
            if (query === '' || title.includes(query) || content.includes(query) || tag.includes(query)) {
                card.style.display = 'flex';
            } else {
                card.style.display = 'none';
            }
        });
    }

    btnKbSearch.addEventListener('click', performKbSearch);
    kbSearchInput.addEventListener('keyup', performKbSearch);";

$replace_js = "    // --- Knowledge Base Search Logic ---
    const kbSearchInput = document.getElementById('kb-search-input');
    const btnKbSearch = document.getElementById('btn-kb-search');
    const kbList = document.getElementById('kb-list');

    let originalKbHtml = kbList.innerHTML;

    async function performKbSearch() {
        const query = kbSearchInput.value.trim();
        
        if (query === '') {
            kbList.innerHTML = originalKbHtml;
            return;
        }
        
        // Show loading state
        btnKbSearch.disabled = true;
        btnKbSearch.innerHTML = '<span class=\"typing-indicator\" style=\"display:flex; padding:0; height:18px;\"><span></span><span></span><span></span></span>';
        kbList.innerHTML = '<div style=\"color: #94a3b8; text-align: center; padding: 2rem;\">Searching local knowledge base and web...</div>';

        try {
            const res = await fetch('/api/kb/search', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ query })
            });
            const data = await res.json();
            
            kbList.innerHTML = '';
            let foundAny = false;

            // Render Local Result (Top 1)
            if (data.local && data.local.length > 0) {
                foundAny = true;
                data.local.forEach(kb => {
                    kbList.innerHTML += `
                        <div class=\"kb-card\" onclick=\"this.querySelector('.kb-content').classList.toggle('expanded')\">
                            <div class=\"kb-header-row\">
                                <span class=\"kb-tag\">\${kb.category}</span>
                                <span class=\"kb-title\">\${kb.title}</span>
                            </div>
                            <div class=\"kb-content\">\${kb.content}</div>
                        </div>
                    `;
                });
            }

            // Render Web Results
            if (data.web && data.web.length > 0) {
                data.web.forEach(web => {
                    if (web.error) return; // Skip errors
                    foundAny = true;
                    // Web cards have primary border and source link
                    kbList.innerHTML += `
                        <div class=\"kb-card web-card\" onclick=\"window.open('\${web.source}', '_blank')\" style=\"border-color: rgba(167, 139, 250, 0.4);\">
                            <div class=\"kb-header-row\">
                                <span class=\"kb-tag\" style=\"color: #a78bfa; border-color: rgba(167, 139, 250, 0.4);\">\${web.category}</span>
                                <span class=\"kb-title\">\${web.title}</span>
                            </div>
                            <div class=\"kb-content\">\${web.content}</div>
                            <div style=\"font-size: 0.8rem; color: #64748b; margin-top: 0.5rem; word-break: break-all;\">Source: \${web.source}</div>
                        </div>
                    `;
                });
            }

            if (!foundAny) {
                kbList.innerHTML = '<div style=\"color: #94a3b8; text-align: center; padding: 2rem;\">No local or web articles found for \"' + query + '\".</div>';
            }

        } catch (error) {
            console.error(error);
            kbList.innerHTML = '<div style=\"color: #ef4444; text-align: center; padding: 2rem;\">Search failed to connect to server.</div>';
        } finally {
            btnKbSearch.disabled = false;
            btnKbSearch.innerHTML = 'Search';
        }
    }

    btnKbSearch.addEventListener('click', performKbSearch);
    kbSearchInput.addEventListener('keypress', e => { if (e.key === 'Enter') performKbSearch(); });";

$content = str_replace($search_js, $replace_js, $content);
file_put_contents($file, $content);
echo "Twig updated.\n";
