/**
 * Japs Transfort - Central Application JavaScript
 * Handles password visibility toggling, live complexity checklist,
 * and password confirmation matching.
 */
(function () {
    'use strict';

    document.addEventListener('DOMContentLoaded', function () {
        initPasswordToggles();
        initPasswordChecklist();
        initRegisterSubmit();
        initCatalogMultiSelect();
        initCartCheckboxes();
    });

    /**
     * Show / Hide password visibility toggle for password inputs with an eye button.
     */
    function initPasswordToggles() {
        document.querySelectorAll('.toggle-password').forEach(function (button) {
            button.addEventListener('click', function () {
                var inputGroup = button.closest('.input-group');
                if (!inputGroup) return;
                var input = inputGroup.querySelector('input');
                if (!input) return;
                var icon = button.querySelector('i');

                if (input.type === 'password') {
                    input.type = 'text';
                    if (icon) {
                        icon.classList.remove('bi-eye');
                        icon.classList.add('bi-eye-slash');
                    }
                } else {
                    input.type = 'password';
                    if (icon) {
                        icon.classList.remove('bi-eye-slash');
                        icon.classList.add('bi-eye');
                    }
                }
            });
        });
    }

    /**
     * Live checklist updating for new-password fields and confirmation match indicator.
     */
    function initPasswordChecklist() {
        var pwdInput = document.getElementById('password');
        var checklist = document.getElementById('passwordChecklist');
        var confirmInput = document.getElementById('confirmPassword');
        var matchIndicator = document.getElementById('confirmPasswordMatch');

        // Only initialize if a checklist or confirm field exists on the page
        if (!pwdInput || (!checklist && !confirmInput)) {
            return;
        }

        function hasSpecialChar(val) {
            for (var i = 0; i < val.length; i++) {
                var code = val.charCodeAt(i);
                var char = val.charAt(i);
                if (code >= 33 && code <= 126 && !/[a-zA-Z0-9]/.test(char)) {
                    return true;
                }
            }
            return false;
        }

        function isAllPrintableAscii(val) {
            if (val.length === 0) return false;
            for (var i = 0; i < val.length; i++) {
                var code = val.charCodeAt(i);
                if (code < 32 || code > 126) {
                    return false;
                }
            }
            return true;
        }

        function updateRules() {
            var val = pwdInput.value || '';
            var isBlank = val.length === 0;

            var rules = {
                length: val.length >= 8 && val.length <= 64,
                upper: /[A-Z]/.test(val),
                lower: /[a-z]/.test(val),
                digit: /[0-9]/.test(val),
                special: hasSpecialChar(val),
                whitespace: !isBlank && !/\s/.test(val),
                ascii: isAllPrintableAscii(val)
            };

            if (checklist) {
                Object.keys(rules).forEach(function (ruleKey) {
                    var item = checklist.querySelector('[data-rule="' + ruleKey + '"]');
                    if (!item) return;
                    var icon = item.querySelector('i');
                    var passed = rules[ruleKey];

                    item.classList.remove('text-success', 'text-danger', 'text-muted');
                    if (icon) {
                        icon.classList.remove('bi-check-circle-fill', 'bi-x-circle', 'bi-circle');
                    }

                    if (passed) {
                        item.classList.add('text-success');
                        if (icon) icon.classList.add('bi-check-circle-fill');
                    } else if (isBlank) {
                        item.classList.add('text-muted');
                        if (icon) icon.classList.add('bi-circle');
                    } else {
                        item.classList.add('text-danger');
                        if (icon) icon.classList.add('bi-x-circle');
                    }
                });
            }

            updateMatch();
        }

        function updateMatch() {
            if (!confirmInput || !matchIndicator) return;
            var pwdVal = pwdInput.value || '';
            var confirmVal = confirmInput.value || '';

            if (confirmVal.length === 0) {
                matchIndicator.innerHTML = '';
                return;
            }

            if (confirmVal === pwdVal) {
                matchIndicator.innerHTML = '<span class="text-success"><i class="bi bi-check-circle-fill me-1"></i> Passwords match</span>';
            } else {
                matchIndicator.innerHTML = '<span class="text-danger"><i class="bi bi-x-circle me-1"></i> Passwords do not match</span>';
            }
        }

        pwdInput.addEventListener('input', updateRules);
        if (confirmInput) {
            confirmInput.addEventListener('input', updateMatch);
        }

        // Initialize state on page load
        updateRules();
    }

    /**
     * Register page: keep the Submit button disabled until all password
     * rules pass AND both password fields match.
     * Only activates when the register form (#jt-reg-form) is present.
     */
    function initRegisterSubmit() {
        var form = document.getElementById('jt-reg-form');
        var submitBtn = document.getElementById('jt-reg-submit');
        var pwdInput = document.getElementById('password');
        var confirmInput = document.getElementById('confirmPassword');

        if (!form || !submitBtn || !pwdInput || !confirmInput) {
            return;
        }

        function hasSpecialChar(val) {
            for (var i = 0; i < val.length; i++) {
                var code = val.charCodeAt(i);
                var char = val.charAt(i);
                if (code >= 33 && code <= 126 && !/[a-zA-Z0-9]/.test(char)) {
                    return true;
                }
            }
            return false;
        }

        function isAllPrintableAscii(val) {
            if (val.length === 0) return false;
            for (var i = 0; i < val.length; i++) {
                var code = val.charCodeAt(i);
                if (code < 32 || code > 126) return false;
            }
            return true;
        }

        function allRulesPassed(val) {
            if (!val || val.length === 0) return false;
            return (
                val.length >= 8 && val.length <= 64 &&
                /[A-Z]/.test(val) &&
                /[a-z]/.test(val) &&
                /[0-9]/.test(val) &&
                hasSpecialChar(val) &&
                !/\s/.test(val) &&
                isAllPrintableAscii(val)
            );
        }

        function syncSubmit() {
            var pwdVal = pwdInput.value || '';
            var confirmVal = confirmInput.value || '';
            var ready = allRulesPassed(pwdVal) && confirmVal.length > 0 && confirmVal === pwdVal;
            submitBtn.disabled = !ready;
        }

        pwdInput.addEventListener('input', syncSubmit);
        confirmInput.addEventListener('input', syncSubmit);

        // Initialize on page load (keep disabled unless server round-trip
        // left values populated — e.g. validation error preserving input).
        syncSubmit();
    }

    /**
     * Catalog multi-select handling for bulk adding products to cart.
     */
    function initCatalogMultiSelect() {
        var selectAllCb = document.getElementById('selectAllCatalog');
        var rowChecks = Array.from(document.querySelectorAll('.catalog-row-check'));
        var addBtn = document.getElementById('addSelectedBtn');
        var countEl = document.getElementById('selectedCount');
        var container = document.getElementById('bulkSelectedContainer');
        var form = document.getElementById('bulkAddForm');

        if (!form || rowChecks.length === 0) {
            return;
        }

        function syncSelected() {
            if (container) {
                container.innerHTML = '';
            }
            var count = 0;
            rowChecks.forEach(function (cb) {
                if (cb.checked) {
                    count++;
                    if (container) {
                        var tr = cb.closest('tr');
                        var qtyInput = tr ? tr.querySelector('.catalog-item-qty') : null;
                        var qtyVal = qtyInput ? qtyInput.value : '1';

                        var idHidden = document.createElement('input');
                        idHidden.type = 'hidden';
                        idHidden.name = 'selectedIds';
                        idHidden.value = cb.value;
                        container.appendChild(idHidden);

                        var qtyHidden = document.createElement('input');
                        qtyHidden.type = 'hidden';
                        qtyHidden.name = 'quantities';
                        qtyHidden.value = qtyVal;
                        container.appendChild(qtyHidden);
                    }
                }
            });

            if (countEl) {
                countEl.textContent = count;
            }
            if (addBtn) {
                addBtn.disabled = (count === 0);
            }
        }

        if (selectAllCb) {
            selectAllCb.addEventListener('change', function () {
                rowChecks.forEach(function (cb) {
                    cb.checked = selectAllCb.checked;
                });
                syncSelected();
            });
        }

        rowChecks.forEach(function (cb) {
            cb.addEventListener('change', function () {
                if (!cb.checked && selectAllCb) {
                    selectAllCb.checked = false;
                } else if (selectAllCb && rowChecks.length > 0 && rowChecks.every(function (c) { return c.checked; })) {
                    selectAllCb.checked = true;
                }
                syncSelected();
            });
        });

        form.addEventListener('submit', function () {
            syncSelected();
        });

        document.querySelectorAll('.catalog-item-qty').forEach(function (input) {
            input.addEventListener('input', function () {
                var tr = input.closest('tr');
                var cb = tr ? tr.querySelector('.catalog-row-check') : null;
                if (cb && cb.checked) {
                    syncSelected();
                }
            });
        });

        syncSelected();
    }

    /**
     * Cart page: checkbox-driven selection, live totals, "N of M selected" counter,
     * and hidden-input injection for the place-order form.
     */
    function initCartCheckboxes() {
        var selectAllCb = document.getElementById('selectAll');
        var rowChecks   = Array.from(document.querySelectorAll('.row-check'));
        var subtotalEl  = document.getElementById('summarySubtotal');
        var totalEl     = document.getElementById('summaryTotal');
        var placeBtn    = document.getElementById('placeOrderBtn');
        var container   = document.getElementById('selectedIdsContainer');
        var selCountEl  = document.getElementById('cartSelectionCount');

        if (rowChecks.length === 0 || !placeBtn) {
            return;
        }

        function formatPHP(amount) {
            return '\u20B1' + amount.toLocaleString('en-PH', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
        }

        function updateSummary() {
            var sum = 0;
            var checkedCount = 0;
            if (container) container.innerHTML = '';

            rowChecks.forEach(function (cb) {
                var tr = cb.closest('tr');
                var lineTotal = parseFloat(tr ? tr.getAttribute('data-line-total') : 0) || 0;
                if (cb.checked) {
                    sum += lineTotal;
                    checkedCount++;
                    if (container) {
                        var hidden = document.createElement('input');
                        hidden.type  = 'hidden';
                        hidden.name  = 'selectedIds';
                        hidden.value = cb.value;
                        container.appendChild(hidden);
                    }
                }
            });

            if (subtotalEl) subtotalEl.textContent = formatPHP(sum);
            if (totalEl)    totalEl.textContent    = formatPHP(sum);
            placeBtn.disabled = (checkedCount === 0);

            if (selCountEl) {
                selCountEl.textContent = checkedCount + ' of ' + rowChecks.length + ' item' + (rowChecks.length !== 1 ? 's' : '') + ' selected';
            }
        }

        if (selectAllCb) {
            selectAllCb.addEventListener('change', function () {
                rowChecks.forEach(function (cb) { cb.checked = selectAllCb.checked; });
                updateSummary();
            });
        }

        rowChecks.forEach(function (cb) {
            cb.addEventListener('change', function () {
                if (selectAllCb) {
                    selectAllCb.checked = rowChecks.every(function (c) { return c.checked; });
                }
                updateSummary();
            });
        });

        // Initialize on page load (all checked by default)
        updateSummary();
    }
})();
