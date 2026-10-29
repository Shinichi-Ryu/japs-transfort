/**
 * Jappy Floating Help Assistant
 * Reusable client-side logic for the admin and reseller portals.
 * Strictly uses role-aware backend help search (GET /help/search?q=...) without touching business database records.
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
    var MAX_HISTORY = 20;

    var panelOpen = false;
    var lastActiveElement = null;
    var greetingTimer = null;
    var isSubmitting = false;

    // DOM Elements
    var elContainer = null;
    var elBtn = null;
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

    // Render Assistant Message
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
                submitQuestion(q);
            });
            elChips.appendChild(btn);
        });
    }

    // Build Assistant Answer HTML from API response results
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

    // Submit Question to /help/search
    function submitQuestion(questionText) {
        if (!questionText || isSubmitting) return;
        var query = questionText.trim();
        if (!query) return;

        isSubmitting = true;
        if (elInput) elInput.value = '';
        if (elSendBtn) elSendBtn.disabled = true;

        // Render user question
        renderUserMessage(query);
        appendToHistory({ sender: 'user', text: query });

        // Show thinking avatar and typing dots
        showTypingIndicator();

        // Delay ~450ms for realistic thinking animation
        setTimeout(function () {
            var url = '/help/search?q=' + encodeURIComponent(query);
            fetch(url, {
                method: 'GET',
                headers: {
                    'Accept': 'application/json'
                }
            })
            .then(function (res) {
                if (!res.ok) {
                    throw new Error('Search failed with status: ' + res.status);
                }
                return res.json();
            })
            .then(function (data) {
                hideTypingIndicator();
                isSubmitting = false;
                if (elSendBtn) elSendBtn.disabled = false;

                var mood = data.avatarMood || (data.found ? 'happy' : 'surprised');
                var answerHtml = buildAnswerHtml(data);

                renderAssistantMessage(mood, answerHtml);

                // Update quick chips if provided
                if (data.quickChips) {
                    renderChips(data.quickChips);
                }

                // Save assistant message to history
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
            .catch(function (err) {
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
        }, 450);
    }

    // Initial Load of Quick Chips & Greeting
    function loadInitialHelp() {
        var history = loadHistory();
        if (history && history.length > 0) {
            // Replay existing session history
            history.forEach(function (item) {
                if (item.sender === 'user') {
                    renderUserMessage(item.text);
                } else {
                    renderAssistantMessage(item.mood || 'happy', item.html || formatAnswerText(item.text));
                    if (item.chips) {
                        renderChips(item.chips);
                    }
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
            // Offline/fallback welcome
            var fallbackHtml = '<div class="jt-help-answer-text">Hi! Need assistance? Ask me a question below.</div>';
            renderAssistantMessage('hello', fallbackHtml);
        });
    }

    // Toggle Panel Open/Close
    function togglePanel(open) {
        var willOpen = typeof open === 'boolean' ? open : !panelOpen;
        if (willOpen === panelOpen) return;

        panelOpen = willOpen;

        if (panelOpen) {
            // Dismiss greeting if open
            dismissGreeting();

            try {
                lastActiveElement = document.activeElement;
            } catch (e) {}

            elPanel.style.display = 'flex';
            elPanel.setAttribute('aria-modal', 'true');
            elBtn.setAttribute('aria-expanded', 'true');

            // Populate initial mascot heads if empty
            if (elHeaderAvatar && !elHeaderAvatar.hasChildNodes()) {
                elHeaderAvatar.innerHTML = getMascotSvg('hello', '40px');
            }

            // If messages are empty, load initial help
            if (elMessages && elMessages.children.length === 0) {
                loadInitialHelp();
            } else {
                scrollToBottom();
            }

            // Focus input or close button
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

        elBtn = document.getElementById('jt-help-btn');
        elBtnMascot = document.getElementById('jt-help-btn-mascot');
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

        // Toggle button click
        if (elBtn) {
            elBtn.addEventListener('click', function () {
                togglePanel();
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

        // Form submit
        if (elForm && elInput) {
            elForm.addEventListener('submit', function (e) {
                e.preventDefault();
                submitQuestion(elInput.value);
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
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }

    // Public API
    global.jtHelpAssistant = {
        open: function () { togglePanel(true); },
        close: function () { togglePanel(false); },
        toggle: function () { togglePanel(); },
        ask: function (q) {
            togglePanel(true);
            submitQuestion(q);
        }
    };

})(window);
