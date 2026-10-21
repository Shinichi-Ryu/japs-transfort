/**
 * Japs Transfort - Central Table View Controller (table-view.js)
 * Provides:
 * 1. Compact View (default): fits screen without horizontal scroll.
 * 2. Column Visibility Dropdown: dynamic checkbox list per table with localStorage persistence,
 *    fixed-viewport collision avoidance, sticky Reset row, and scroll isolation.
 */
(function() {
    'use strict';

    function getTableSettings(tableId) {
        try {
            return JSON.parse(localStorage.getItem('tableView:' + tableId) || '{}');
        } catch (e) {
            return {};
        }
    }

    function saveTableSettings(tableId, settings) {
        try {
            localStorage.setItem('tableView:' + tableId, JSON.stringify(settings));
        } catch (e) {}
    }

    function setColVisibility(table, colId, visible) {
        var cells = table.querySelectorAll('[data-col="' + colId + '"]');
        cells.forEach(function(cell) {
            cell.classList.toggle('jt-col-hidden', !visible);
        });
    }

    function closeAllDropdowns() {
        document.querySelectorAll('.jt-cols-menu.show').forEach(function(menu) {
            menu.classList.remove('show');
        });
        document.querySelectorAll('.jt-cols-btn.show').forEach(function(btn) {
            btn.classList.remove('show');
            btn.setAttribute('aria-expanded', 'false');
        });
    }

    function positionDropdownMenu(btn, menu) {
        if (!btn || !menu) return;

        var btnRect = btn.getBoundingClientRect();
        if (btnRect.width === 0 && btnRect.height === 0) return;

        var menuWidth = menu.offsetWidth || 230;
        var menuHeight = menu.offsetHeight || 280;

        var vpWidth = window.innerWidth || document.documentElement.clientWidth;
        var vpHeight = window.innerHeight || document.documentElement.clientHeight;

        // Horizontal: align right edge of menu with button's right edge
        var left = btnRect.right - menuWidth;
        if (left + menuWidth > vpWidth - 10) {
            left = vpWidth - menuWidth - 10;
        }
        if (left < 10) {
            left = 10;
        }

        // Vertical: check space below vs space above
        var margin = 6;
        var spaceBelow = vpHeight - btnRect.bottom - margin;
        var spaceAbove = btnRect.top - margin;
        var maxHeight = Math.min(vpHeight * 0.6, 320);

        var top;
        if (spaceBelow >= Math.min(menuHeight, maxHeight) || spaceBelow >= spaceAbove) {
            // Open downward
            top = btnRect.bottom + margin;
            var availableHeight = Math.min(spaceBelow - 6, maxHeight);
            menu.style.maxHeight = Math.max(availableHeight, 140) + 'px';
        } else {
            // Open upward
            var availableHeight = Math.min(spaceAbove - 6, maxHeight);
            var finalHeight = Math.min(menuHeight, availableHeight);
            top = btnRect.top - finalHeight - margin;
            menu.style.maxHeight = Math.max(availableHeight, 140) + 'px';
        }

        menu.style.position = 'fixed';
        menu.style.zIndex = '99999';
        menu.style.left = Math.round(left) + 'px';
        menu.style.top = Math.round(top) + 'px';
        menu.style.right = 'auto';
        menu.style.bottom = 'auto';
    }

    function initTableView(wrap) {
        var tableId = wrap.getAttribute('data-table-id');
        if (!tableId) return;

        var table = wrap.querySelector('table.jt-table') || wrap.querySelector('table');
        if (!table) return;

        var controls = wrap.querySelector('.jt-table-controls') ||
                       document.querySelector('.jt-table-controls[data-table-id="' + tableId + '"]');
        var colsBtn = controls ? controls.querySelector('.jt-cols-btn') : null;
        var colsMenu = controls ? controls.querySelector('.jt-cols-menu') : null;
        var colsList = colsMenu ? colsMenu.querySelector('.jt-cols-list') : null;
        var resetBtn = colsMenu ? colsMenu.querySelector('.jt-cols-reset') : null;

        var settings = getTableSettings(tableId);
        var hiddenCols = Array.isArray(settings.hiddenCols) ? settings.hiddenCols.slice() : [];

        // Populate and bind column dropdown
        var ths = table.querySelectorAll('thead th[data-col]');
        if (ths.length > 0 && colsList) {
            colsList.innerHTML = '';
            ths.forEach(function(th) {
                var colId = th.getAttribute('data-col');
                if (!colId) return;

                var colName = th.getAttribute('data-col-name') ||
                              th.innerText.replace(/[\r\n\t]+/g, ' ').trim() ||
                              colId;

                var label = document.createElement('label');
                label.className = 'dropdown-item d-flex align-items-center gap-2 py-1 px-3 m-0';
                label.style.cursor = 'pointer';

                var cb = document.createElement('input');
                cb.type = 'checkbox';
                cb.className = 'form-check-input mt-0';
                cb.setAttribute('data-col-id', colId);
                cb.checked = hiddenCols.indexOf(colId) === -1;

                var span = document.createElement('span');
                span.textContent = colName;
                span.className = 'small text-truncate';

                label.appendChild(cb);
                label.appendChild(span);
                colsList.appendChild(label);

                cb.addEventListener('change', function() {
                    setColVisibility(table, colId, cb.checked);
                    var idx = hiddenCols.indexOf(colId);
                    if (!cb.checked && idx === -1) {
                        hiddenCols.push(colId);
                    } else if (cb.checked && idx !== -1) {
                        hiddenCols.splice(idx, 1);
                    }
                    settings.hiddenCols = hiddenCols;
                    saveTableSettings(tableId, settings);
                });

                // Apply saved hidden state
                if (hiddenCols.indexOf(colId) !== -1) {
                    setColVisibility(table, colId, false);
                }
            });
        }

        // Columns button click toggle
        if (colsBtn && colsMenu) {
            colsBtn.addEventListener('click', function(e) {
                e.preventDefault();
                e.stopPropagation();

                // Teleport menu to document.body so no ancestor card or transform can clip or offset it
                if (colsMenu.parentElement !== document.body) {
                    document.body.appendChild(colsMenu);
                }

                var isOpen = colsMenu.classList.contains('show');
                if (isOpen) {
                    colsMenu.classList.remove('show');
                    colsBtn.classList.remove('show');
                    colsBtn.setAttribute('aria-expanded', 'false');
                } else {
                    closeAllDropdowns();
                    colsMenu.classList.add('show');
                    colsBtn.classList.add('show');
                    colsBtn.setAttribute('aria-expanded', 'true');
                    positionDropdownMenu(colsBtn, colsMenu);
                }
            });

            // Prevent dropdown from closing when clicking inside menu (except reset)
            colsMenu.addEventListener('click', function(e) {
                if (!e.target.closest('.jt-cols-reset')) {
                    e.stopPropagation();
                }
            });
        }

        // Bind Reset button
        if (resetBtn) {
            resetBtn.addEventListener('click', function(e) {
                e.preventDefault();
                e.stopPropagation();
                hiddenCols = [];
                settings = { hiddenCols: [] };
                saveTableSettings(tableId, settings);

                ths.forEach(function(th) {
                    var colId = th.getAttribute('data-col');
                    if (colId) setColVisibility(table, colId, true);
                });

                if (colsList) {
                    colsList.querySelectorAll('input[type="checkbox"]').forEach(function(c) {
                        c.checked = true;
                    });
                }
            });
        }
    }

    // Dismiss dropdown on outside clicks
    document.addEventListener('click', function(e) {
        if (!e.target.closest('.jt-cols-dropdown') && !e.target.closest('.jt-cols-menu') && !e.target.closest('.jt-cols-btn')) {
            closeAllDropdowns();
        }
    });

    // Dismiss dropdown on Escape key
    document.addEventListener('keydown', function(e) {
        if (e.key === 'Escape' || e.key === 'Esc') {
            closeAllDropdowns();
        }
    });

    // Dismiss dropdown on window resize
    window.addEventListener('resize', closeAllDropdowns);

    // Dismiss dropdown only on page scroll
    var lastScrollY = window.pageYOffset || document.documentElement.scrollTop;
    window.addEventListener('scroll', function() {
        var currentScrollY = window.pageYOffset || document.documentElement.scrollTop;
        if (Math.abs(currentScrollY - lastScrollY) > 15) {
            lastScrollY = currentScrollY;
            closeAllDropdowns();
        }
    }, { passive: true });

    // Initialize all tables on document ready
    function initAll() {
        var wraps = document.querySelectorAll('.jt-table-wrap[data-table-id]');
        wraps.forEach(function(wrap) {
            initTableView(wrap);
        });
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initAll);
    } else {
        initAll();
    }
})();
