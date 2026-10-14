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
})();
