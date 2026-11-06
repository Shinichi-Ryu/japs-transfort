/**
 * Jappy Floating Help Assistant
 * Reusable client-side logic for the admin and reseller portals.
 * Strictly uses role-aware backend help search and Gemini AI without touching business database records.
 */
(function (global) {
    'use strict';

    // Safe Browser Storage
    var SafeStorage = {
        get: function (key) {
            try {
                return sessionStorage.getItem(key);
            } catch (e) {
                return null;
            }
        },
        set: function (key, val) {
            try {
                sessionStorage.setItem(key, val);
            } catch (e) {}
        },
        remove: function (key) {
            try {
                sessionStorage.removeItem(key);
            } catch (e) {}
        }
    };

    var STORAGE_KEY_HISTORY = 'jt_jappy_chat_history';
    var STORAGE_KEY_GREETING = 'jt_jappy_greeting_dismissed';
    var STORAGE_KEY_COLLAPSED = 'jt_jappy_collapsed';
    var STORAGE_KEY_HIDDEN = 'jt_jappy_hidden';
    var MAX_HISTORY = 20;

    // Persistent storage (localStorage) for collapsed state across sessions
    var PersistStorage = {
        get: function (key) {
            try {
                return localStorage.getItem(key);
            } catch (e) {
                return null;
            }
        },
        set: function (key, val) {
            try {
                localStorage.setItem(key, val);
            } catch (e) {}
        }
    };

    var panelOpen = false;
    var jappyCollapsed = false;
    var jappyHidden = false;
    var lastActiveElement = null;
    var greetingTimer = null;
    var isSubmitting = false;

    // DOM Elements
    var elContainer = null;
    var elTriggerWrap = null;
    var elBtn = null;
    var elCollapseBtn = null;
    var elUnhideBtn = null;
    var elBtnMascot = null;
    var elGreeting = null;
    var elGreetingClose = null;
    var elGreetingTrigger = null;
    var elPanel = null;
    var elHeaderAvatar = null;
    var elCloseBtn = null;
    var elClearBtn = null;
    var elMessages = null;
    var elChipsWrap = null;
    var elChips = null;
    var elForm = null;
    var elInput = null;
    var elSendBtn = null;

    function getMascotSvg(mood, size, extraClass) {
        if (global.Jappy && typeof global.Jappy.svg === 'function') {
            return global.Jappy.svg(mood, size, extraClass);
        }
        // Fallback placeholder if Jappy is still loading
        return '<svg viewBox="0 0 140 140" style="width:' + (size || '40px') + ';height:' + (size || '40px') + ';"><circle cx="70" cy="70" r="50" fill="#f2b632"/></svg>';
    }

    function escapeHtml(text) {
        if (!text) return '';
        return String(text)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#039;');
    }

    function formatAnswerText(text) {
        if (!text) return '';
        var safe = escapeHtml(text);
        // Replace **bold** with <strong>bold</strong>
        safe = safe.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>');
        return safe;
    }

    function getCsrfInfo() {
        var token = null;
        var header = 'X-CSRF-TOKEN';

        if (elContainer) {
            token = elContainer.getAttribute('data-csrf-token');
            var h = elContainer.getAttribute('data-csrf-header');
            if (h) header = h;
        }

        if (!token) {
            var metaToken = document.querySelector('meta[name="_csrf"]');
            var metaHeader = document.querySelector('meta[name="_csrf_header"]');
            if (metaToken) token = metaToken.getAttribute('content');
            if (metaHeader) header = metaHeader.getAttribute('content') || header;
        }

        if (!token) {
            var inputToken = document.querySelector('input[name="_csrf"]');
            if (inputToken) token = inputToken.value;
        }

        return { token: token, header: header };
    }

    // Chat History Management (Session Storage, up to 20 messages)
    function loadHistory() {
        var raw = SafeStorage.get(STORAGE_KEY_HISTORY);
        if (!raw) return [];
        try {
            var parsed = JSON.parse(raw);
            if (Array.isArray(parsed)) {
                return parsed.slice(-MAX_HISTORY);
            }
        } catch (e) {}
        return [];
    }

    function saveHistory(messages) {
        if (!Array.isArray(messages)) return;
        var trimmed = messages.slice(-MAX_HISTORY);
        SafeStorage.set(STORAGE_KEY_HISTORY, JSON.stringify(trimmed));
    }

    function appendToHistory(msg) {
        var list = loadHistory();
        list.push(msg);
        saveHistory(list);
    }

    function clearHistory() {
        SafeStorage.remove(STORAGE_KEY_HISTORY);
        if (elMessages) {
            elMessages.innerHTML = '';
        }
        loadInitialHelp();
    }

    // Scroll to bottom of message view
    function scrollToBottom() {
        if (elMessages) {
            elMessages.scrollTop = elMessages.scrollHeight;
        }
    }

    // Render User Message
    function renderUserMessage(text) {
        var row = document.createElement('div');
        row.className = 'jt-help-msg jt-help-msg-user';
        row.innerHTML = '<div class="jt-help-bubble">' + escapeHtml(text) + '</div>';
        elMessages.appendChild(row);
        scrollToBottom();
    }

    // Render Assistant Message for keyword search html
    function renderAssistantMessage(mood, contentHtml) {
        var row = document.createElement('div');
        row.className = 'jt-help-msg jt-help-msg-assistant';

        var avatarHtml = '<div class="jt-help-avatar" aria-hidden="true">' +
            getMascotSvg(mood || 'happy', '32px') +
            '</div>';

        row.innerHTML = avatarHtml + '<div class="jt-help-bubble">' + contentHtml + '</div>';
        elMessages.appendChild(row);
        scrollToBottom();
    }

    // Render Safe Answer Message (Strictly avoids innerHTML for untrusted model text)
    function renderSafeAnswerMessage(mood, data) {
        var row = document.createElement('div');
        row.className = 'jt-help-msg jt-help-msg-assistant';

        var avatarDiv = document.createElement('div');
        avatarDiv.className = 'jt-help-avatar';
        avatarDiv.setAttribute('aria-hidden', 'true');
        avatarDiv.innerHTML = getMascotSvg(mood || 'happy', '32px');
        row.appendChild(avatarDiv);

        var bubbleDiv = document.createElement('div');
        bubbleDiv.className = 'jt-help-bubble';

        var textContainer = document.createElement('div');
        textContainer.className = 'jt-help-answer-text';

        // Split text by newlines and safely parse **bold** into text nodes & <strong> elements
        var rawText = (data && data.answer) ? data.answer : '';
        var lines = rawText.split('\n');

        lines.forEach(function (line, idx) {
            if (idx > 0) {
                textContainer.appendChild(document.createElement('br'));
            }
            var parts = line.split(/(\*\*.*?\*\*)/g);
            parts.forEach(function (part) {
                if (part.startsWith('**') && part.endsWith('**') && part.length >= 4) {
                    var strong = document.createElement('strong');
                    strong.textContent = part.slice(2, -2);
                    textContainer.appendChild(strong);
                } else if (part) {
                    textContainer.appendChild(document.createTextNode(part));
                }
            });
        });

        bubbleDiv.appendChild(textContainer);

        // Render links from server as normal anchors
        if (data && data.links && data.links.length > 0) {
            var linksWrap = document.createElement('div');
            linksWrap.className = 'jt-help-links-wrap';

            data.links.forEach(function (lnk) {
                if (!lnk || !lnk.url) return;
                var a = document.createElement('a');
                a.className = 'jt-help-link-btn';
                a.href = lnk.url;
                a.textContent = lnk.label || 'Open Page';

                var icon = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
                icon.setAttribute('viewBox', '0 0 24 24');
                icon.setAttribute('width', '12');
                icon.setAttribute('height', '12');
                icon.setAttribute('fill', 'none');
                icon.setAttribute('stroke', 'currentColor');
                icon.setAttribute('stroke-width', '2.5');
                icon.setAttribute('stroke-linecap', 'round');
                icon.setAttribute('stroke-linejoin', 'round');
                icon.setAttribute('aria-hidden', 'true');
                icon.style.marginLeft = '4px';

                var path = document.createElementNS('http://www.w3.org/2000/svg', 'path');
                path.setAttribute('d', 'M7 17l9.2-9.2M17 17V8H8');
                icon.appendChild(path);

                a.appendChild(icon);
                linksWrap.appendChild(a);
            });

            bubbleDiv.appendChild(linksWrap);
        }

        row.appendChild(bubbleDiv);
        elMessages.appendChild(row);
        scrollToBottom();
    }

    // Render Typing Indicator
    function showTypingIndicator() {
        var row = document.createElement('div');
        row.id = 'jt-help-typing-indicator';
        row.className = 'jt-help-msg jt-help-msg-assistant';

        var avatarHtml = '<div class="jt-help-avatar" aria-hidden="true">' +
            getMascotSvg('thinking', '32px') +
            '</div>';

        var dotsHtml = '<div class="jt-help-bubble"><div class="jt-help-dots"><span></span><span></span><span></span></div></div>';
        row.innerHTML = avatarHtml + dotsHtml;
        elMessages.appendChild(row);
        scrollToBottom();
    }

    function hideTypingIndicator() {
        var indicator = document.getElementById('jt-help-typing-indicator');
        if (indicator && indicator.parentNode) {
            indicator.parentNode.removeChild(indicator);
        }
    }

    // Render Quick Chips
    function renderChips(chipsList) {
        if (!elChips || !elChipsWrap) return;

        if (!chipsList || chipsList.length === 0) {
            elChipsWrap.style.display = 'none';
            return;
        }

        elChipsWrap.style.display = 'block';
        elChips.innerHTML = '';

        chipsList.forEach(function (chip) {
            var btn = document.createElement('button');
            btn.type = 'button';
            btn.className = 'jt-help-chip';
            btn.textContent = chip.title;
            btn.setAttribute('data-query', chip.query || chip.title);
            btn.addEventListener('click', function () {
                var q = this.getAttribute('data-query');
                submitChipQuestion(q);
            });
            elChips.appendChild(btn);
        });
    }

    // Build Assistant Answer HTML from GET /help/search results
    function buildAnswerHtml(data) {
        var html = '';

        if (data.message) {
            html += '<div class="jt-help-answer-text">' + formatAnswerText(data.message) + '</div>';
        }

        if (data.results && data.results.length > 0) {
            data.results.forEach(function (res) {
                html += '<div class="jt-help-answer-item">';
                if (res.title) {
                    html += '<div class="jt-help-item-title">' + escapeHtml(res.title) + '</div>';
                }
                if (res.answer) {
                    html += '<div class="jt-help-answer-text">' + formatAnswerText(res.answer) + '</div>';
                }
                if (res.link) {
                    var label = res.linkLabel ? escapeHtml(res.linkLabel) : 'Open Page';
                    html += '<a href="' + escapeHtml(res.link) + '" class="jt-help-link-btn">' +
                        label +
                        ' <svg viewBox="0 0 24 24" width="12" height="12" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M7 17l9.2-9.2M17 17V8H8"/></svg>' +
                        '</a>';
                }
                html += '</div>';
            });
        }

        return html;
    }

    // Submit Instant Quick Chip Question (GET /help/search)
    function submitChipQuestion(questionText) {
        if (!questionText || isSubmitting) return;
        var query = questionText.trim();
        if (!query) return;

        isSubmitting = true;
        if (elInput) elInput.value = '';
        if (elSendBtn) elSendBtn.disabled = true;

        renderUserMessage(query);
        appendToHistory({ sender: 'user', text: query });
        showTypingIndicator();

        setTimeout(function () {
            var url = '/help/search?q=' + encodeURIComponent(query);
            fetch(url, {
                method: 'GET',
                headers: { 'Accept': 'application/json' }
            })
            .then(function (res) {
                if (!res.ok) throw new Error('Search failed with status: ' + res.status);
                return res.json();
            })
            .then(function (data) {
                hideTypingIndicator();
                isSubmitting = false;
                if (elSendBtn) elSendBtn.disabled = false;

                var mood = data.avatarMood || (data.found ? 'happy' : 'surprised');
                var answerHtml = buildAnswerHtml(data);
                renderAssistantMessage(mood, answerHtml);

                if (data.quickChips) {
                    renderChips(data.quickChips);
                }

                appendToHistory({
                    sender: 'assistant',
                    mood: mood,
                    html: answerHtml,
                    chips: data.quickChips || []
                });

                if (elInput) {
                    try { elInput.focus(); } catch (e) {}
                }
            })
            .catch(function () {
                hideTypingIndicator();
                isSubmitting = false;
                if (elSendBtn) elSendBtn.disabled = false;

                var errorHtml = '<div class="jt-help-answer-text">Sorry, I had trouble reaching the help knowledge base. Please try again!</div>';
                renderAssistantMessage('worried', errorHtml);
                appendToHistory({
                    sender: 'assistant',
                    mood: 'worried',
                    html: errorHtml
                });
            });
        }, 300);
    }

    // Submit Typed Question (POST /help/ask with AI & safe fallback)
    function submitTypedQuestion(questionText) {
        if (!questionText || isSubmitting) return;
        var query = questionText.trim();
        if (!query) return;

        isSubmitting = true;
        if (elInput) elInput.value = '';
        if (elSendBtn) elSendBtn.disabled = true;

        renderUserMessage(query);

        // Prepare up to 6 last messages from existing history before appending current
        var existingHistory = loadHistory();
        var historyPayload = [];
        var startIdx = Math.max(0, existingHistory.length - 6);
        for (var i = startIdx; i < existingHistory.length; i++) {
            var item = existingHistory[i];
            var role = (item.sender === 'user') ? 'user' : 'assistant';
            var txt = item.plainText || item.text || '';
            if (txt) {
                historyPayload.push({
                    role: role,
                    text: txt.substring(0, 500)
                });
            }
        }

        appendToHistory({ sender: 'user', text: query });
        showTypingIndicator();

        // 20-second client timeout with retry message
        var timedOut = false;
        var abortController = (typeof AbortController !== 'undefined') ? new AbortController() : null;
        var timeoutTimer = setTimeout(function () {
            timedOut = true;
            if (abortController) {
                try { abortController.abort(); } catch (e) {}
            }
            hideTypingIndicator();
            isSubmitting = false;
            if (elSendBtn) elSendBtn.disabled = false;

            var retryMsg = 'Sorry, that took a bit too long. Please try asking again!';
            renderSafeAnswerMessage('surprised', { answer: retryMsg, links: [] });
            appendToHistory({
                sender: 'assistant',
                mood: 'surprised',
                plainText: retryMsg
            });
        }, 20000);

        var csrf = getCsrfInfo();
        var headers = {
            'Content-Type': 'application/json',
            'Accept': 'application/json'
        };
        if (csrf.token) {
            headers[csrf.header] = csrf.token;
        }

        fetch('/help/ask', {
            method: 'POST',
            headers: headers,
            body: JSON.stringify({
                message: query.substring(0, 500),
                history: historyPayload
            }),
            signal: abortController ? abortController.signal : undefined
        })
        .then(function (res) {
            clearTimeout(timeoutTimer);
            if (timedOut) return null;
            if (!res.ok) {
                throw new Error('Ask failed with status: ' + res.status);
            }
            return res.json();
        })
        .then(function (data) {
            if (!data || timedOut) return;
            hideTypingIndicator();
            isSubmitting = false;
            if (elSendBtn) elSendBtn.disabled = false;

            var mood = data.mood || 'happy';
            renderSafeAnswerMessage(mood, data);

            appendToHistory({
                sender: 'assistant',
                mood: mood,
                plainText: data.answer,
                links: data.links || [],
                source: data.source || 'ai'
            });

            if (elInput) {
                try { elInput.focus(); } catch (e) {}
            }
        })
        .catch(function () {
            clearTimeout(timeoutTimer);
            if (timedOut) return;
            hideTypingIndicator();
            isSubmitting = false;
            if (elSendBtn) elSendBtn.disabled = false;

            var fallbackMsg = 'Sorry, I had trouble answering that right now. Please try again in a moment!';
            renderSafeAnswerMessage('surprised', { answer: fallbackMsg, links: [] });
            appendToHistory({
                sender: 'assistant',
                mood: 'surprised',
                plainText: fallbackMsg
            });
        });
    }

    // Initial Load of Quick Chips & Greeting
    function loadInitialHelp() {
        var history = loadHistory();
        if (history && history.length > 0) {
            // Replay existing session history
            history.forEach(function (item) {
                if (item.sender === 'user') {
                    renderUserMessage(item.text);
                } else if (item.plainText || item.answer) {
                    renderSafeAnswerMessage(item.mood || 'happy', {
                        answer: item.plainText || item.answer,
                        links: item.links || []
                    });
                } else {
                    renderAssistantMessage(item.mood || 'happy', item.html || formatAnswerText(item.text));
                }
                if (item.chips) {
                    renderChips(item.chips);
                }
            });
            scrollToBottom();
            return;
        }

        // Fetch initial chips and welcome
        fetch('/help/search', {
            method: 'GET',
            headers: { 'Accept': 'application/json' }
        })
        .then(function (res) {
            if (!res.ok) throw new Error('Initial help load failed');
            return res.json();
        })
        .then(function (data) {
            var welcomeHtml = '<div class="jt-help-answer-text">' +
                '<strong>Hi! I\'m Jappy 👋</strong><br>' +
                'I can help you navigate Japs Transfort, answer questions about orders, products, payments, and settings. What would you like to know?' +
                '</div>';

            renderAssistantMessage('hello', welcomeHtml);

            if (data.quickChips) {
                renderChips(data.quickChips);
            }

            appendToHistory({
                sender: 'assistant',
                mood: 'hello',
                html: welcomeHtml,
                chips: data.quickChips || []
            });
        })
        .catch(function () {
            var fallbackHtml = '<div class="jt-help-answer-text">Hi! Need assistance? Ask me a question below.</div>';
            renderAssistantMessage('hello', fallbackHtml);
        });
    }

    // Set mascot hidden / unhidden state
    function setHidden(hidden) {
        jappyHidden = hidden;
        PersistStorage.set(STORAGE_KEY_HIDDEN, hidden ? '1' : '0');

        if (elContainer) {
            if (hidden) {
                elContainer.classList.add('jt-help-container--hidden');
            } else {
                elContainer.classList.remove('jt-help-container--hidden');
            }
        }

        if (elTriggerWrap) {
            elTriggerWrap.style.display = hidden ? 'none' : 'flex';
        }

        if (elUnhideBtn) {
            elUnhideBtn.style.display = hidden ? 'flex' : 'none';
        }

        if (elToggleMascotLabel) {
            elToggleMascotLabel.textContent = hidden ? 'Show mascot' : 'Hide mascot';
        }

        // When hiding, dismiss greeting
        if (hidden) {
            dismissGreeting();
        }
    }

    // Set mascot collapsed / expanded state
    function setCollapsed(collapsed) {
        jappyCollapsed = collapsed;
        PersistStorage.set(STORAGE_KEY_COLLAPSED, collapsed ? '1' : '0');

        if (elContainer) {
            if (collapsed) {
                elContainer.classList.add('jt-help-container--collapsed');
            } else {
                elContainer.classList.remove('jt-help-container--collapsed');
            }
        }

        if (elBtn) {
            if (collapsed) {
                elBtn.classList.add('jt-help-btn--collapsed');
                elBtn.setAttribute('aria-label', 'Open assistant');
                elBtn.title = 'Open Jappy';
            } else {
                elBtn.classList.remove('jt-help-btn--collapsed');
                elBtn.setAttribute('aria-label', 'Open Jappy help assistant');
                elBtn.title = 'Ask Jappy for help';
            }
        }

        if (elTriggerWrap) {
            if (collapsed) {
                elTriggerWrap.classList.add('jt-help-trigger-wrap--collapsed');
            } else {
                elTriggerWrap.classList.remove('jt-help-trigger-wrap--collapsed');
            }
        }

        if (elCollapseBtn) {
            elCollapseBtn.style.display = collapsed ? 'none' : 'flex';
        }

        if (elBtnMascot) {
            var size = collapsed ? '36px' : '48px';
            elBtnMascot.innerHTML = getMascotSvg('hello', size);
        }

        // If collapsing while panel is open, close the panel
        if (collapsed && panelOpen) {
            togglePanel(false);
        }
    }

    // Toggle Panel Open/Close
    function togglePanel(open) {
        var willOpen = typeof open === 'boolean' ? open : !panelOpen;
        if (willOpen === panelOpen) return;

        panelOpen = willOpen;

        if (panelOpen) {
            dismissGreeting();

            // If currently collapsed, expand before opening panel
            if (jappyCollapsed) {
                setCollapsed(false);
            }

            try {
                lastActiveElement = document.activeElement;
            } catch (e) {}

            elPanel.style.display = 'flex';
            elPanel.setAttribute('aria-modal', 'true');
            elBtn.setAttribute('aria-expanded', 'true');

            if (elHeaderAvatar && !elHeaderAvatar.hasChildNodes()) {
                elHeaderAvatar.innerHTML = getMascotSvg('hello', '40px');
            }

            if (elMessages && elMessages.children.length === 0) {
                loadInitialHelp();
            } else {
                scrollToBottom();
            }

            setTimeout(function () {
                try {
                    if (elInput) elInput.focus();
                } catch (e) {}
            }, 50);

        } else {
            elPanel.style.display = 'none';
            elPanel.setAttribute('aria-modal', 'false');
            elBtn.setAttribute('aria-expanded', 'false');

            if (lastActiveElement && typeof lastActiveElement.focus === 'function') {
                try {
                    lastActiveElement.focus();
                } catch (e) {}
            } else if (elBtn) {
                try {
                    elBtn.focus();
                } catch (e) {}
            }
        }
    }

    // Greeting Bubble: appears once per session after ~4 seconds
    function setupGreetingBubble() {
        var dismissed = SafeStorage.get(STORAGE_KEY_GREETING);
        if (dismissed === '1') {
            return;
        }

        greetingTimer = setTimeout(function () {
            if (panelOpen) return;
            var isDismissedNow = SafeStorage.get(STORAGE_KEY_GREETING);
            if (isDismissedNow === '1') return;

            if (elGreeting) {
                elGreeting.style.display = 'flex';
            }
        }, 4000);
    }

    function dismissGreeting() {
        if (greetingTimer) {
            clearTimeout(greetingTimer);
            greetingTimer = null;
        }
        SafeStorage.set(STORAGE_KEY_GREETING, '1');
        if (elGreeting) {
            elGreeting.style.display = 'none';
        }
    }

    // Initialize DOM & Event Listeners
    function init() {
        elContainer = document.getElementById('jt-help-assistant');
        if (!elContainer) return;

        elTriggerWrap = document.getElementById('jt-help-trigger-wrap');
        elBtn = document.getElementById('jt-help-btn');
        elBtnMascot = document.getElementById('jt-help-btn-mascot');
        elUnhideBtn = document.getElementById('jt-help-unhide-btn');
        elGreeting = document.getElementById('jt-help-greeting');
        elGreetingClose = document.getElementById('jt-help-greeting-close');
        elGreetingTrigger = document.getElementById('jt-help-greeting-trigger');
        elPanel = document.getElementById('jt-help-panel');
        elHeaderAvatar = document.getElementById('jt-help-header-avatar');
        elCloseBtn = document.getElementById('jt-help-close-btn');
        elClearBtn = document.getElementById('jt-help-clear-btn');
        elMessages = document.getElementById('jt-help-messages');
        elChipsWrap = document.getElementById('jt-help-chips-wrap');
        elChips = document.getElementById('jt-help-chips');
        elForm = document.getElementById('jt-help-form');
        elInput = document.getElementById('jt-help-input');
        elSendBtn = document.getElementById('jt-help-send-btn');

        // Render mascots
        if (elBtnMascot) {
            elBtnMascot.innerHTML = getMascotSvg('hello', '48px');
        }
        if (elHeaderAvatar) {
            elHeaderAvatar.innerHTML = getMascotSvg('hello', '40px');
        }

        // Toggle button click:
        // - collapsed -> expand (single click expands; second click opens panel)
        // - expanded  -> open/close panel
        if (elBtn) {
            elBtn.addEventListener('click', function () {
                if (jappyCollapsed) {
                    setCollapsed(false);
                } else {
                    togglePanel();
                }
            });
        }

        // Greeting bubble interactions
        if (elGreetingClose) {
            elGreetingClose.addEventListener('click', function (e) {
                e.stopPropagation();
                dismissGreeting();
            });
        }
        if (elGreetingTrigger) {
            elGreetingTrigger.addEventListener('click', function () {
                dismissGreeting();
                togglePanel(true);
            });
            elGreetingTrigger.addEventListener('keydown', function (e) {
                if (e.key === 'Enter' || e.key === ' ') {
                    e.preventDefault();
                    dismissGreeting();
                    togglePanel(true);
                }
            });
        }

        // Panel Close button
        if (elCloseBtn) {
            elCloseBtn.addEventListener('click', function () {
                togglePanel(false);
            });
        }

        // Panel Clear chat button
        if (elClearBtn) {
            elClearBtn.addEventListener('click', function () {
                clearHistory();
            });
        }

        // Form submit calls submitTypedQuestion (POST /help/ask)
        if (elForm && elInput) {
            elForm.addEventListener('submit', function (e) {
                e.preventDefault();
                submitTypedQuestion(elInput.value);
            });
        }

        // Keyboard navigation: Escape key closes panel
        document.addEventListener('keydown', function (e) {
            if (e.key === 'Escape' && panelOpen) {
                e.stopPropagation();
                togglePanel(false);
            }
        });

        // Trigger greeting timer after 4s
        setupGreetingBubble();

        // ---- Collapsed state init ----
        // 1. If localStorage has a saved preference, use it.
        // 2. If no preference and viewport width < 768px, default to collapsed.
        var savedCollapsed = PersistStorage.get(STORAGE_KEY_COLLAPSED);
        var isNarrow = (window.innerWidth < 768);
        if (savedCollapsed === '1' || (savedCollapsed === null && isNarrow)) {
            setCollapsed(true);
        } else {
            setCollapsed(false);
        }

        // Wire hide/collapse and unhide buttons
        elCollapseBtn = document.getElementById('jt-help-collapse-btn');
        if (elCollapseBtn) {
            elCollapseBtn.addEventListener('click', function (e) {
                e.stopPropagation();
                // Hide the mascot completely so it never blocks content
                setHidden(true);
            });
            elCollapseBtn.addEventListener('keydown', function (e) {
                if (e.key === 'Enter' || e.key === ' ') {
                    e.preventDefault();
                    e.stopPropagation();
                    setHidden(true);
                }
            });
        }

        if (elUnhideBtn) {
            elUnhideBtn.addEventListener('click', function (e) {
                e.stopPropagation();
                setHidden(false);
            });
            elUnhideBtn.addEventListener('keydown', function (e) {
                if (e.key === 'Enter' || e.key === ' ') {
                    e.preventDefault();
                    e.stopPropagation();
                    setHidden(false);
                }
            });
        }

        elToggleMascotBtn = document.getElementById('jt-help-toggle-mascot-btn');
        elToggleMascotLabel = document.getElementById('jt-help-toggle-mascot-label');
        if (elToggleMascotBtn) {
            elToggleMascotBtn.addEventListener('click', function (e) {
                e.stopPropagation();
                setHidden(!jappyHidden);
            });
        }

        // Initialize hidden state:
        // On narrow/crowded screens (< 768px), default to completely hidden unless explicitly enabled
        var savedHidden = PersistStorage.get(STORAGE_KEY_HIDDEN);
        var isCrowdedScreen = (window.innerWidth < 768);
        if (savedHidden === '1' || (savedHidden === null && isCrowdedScreen)) {
            setHidden(true);
        } else {
            setHidden(false);
        }
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }

    // Public API
    global.jtHelpAssistant = {
        open: function () { setHidden(false); togglePanel(true); },
        close: function () { togglePanel(false); },
        toggle: function () { togglePanel(); },
        hide: function () { setHidden(true); },
        unhide: function () { setHidden(false); },
        show: function () { setHidden(false); },
        collapse: function () { setCollapsed(true); },
        expand: function () { setCollapsed(false); },
        ask: function (q) {
            setHidden(false);
            togglePanel(true);
            submitTypedQuestion(q);
        }
    };

})(window);
