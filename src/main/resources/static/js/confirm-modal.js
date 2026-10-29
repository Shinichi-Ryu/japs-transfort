/**
 * Japs Transfort - Themed Confirmation & Alert Dialog System
 * Replaces native browser confirm() and alert() dialogs with an accessible,
 * themed modal matching the Japs Transfort UI.
 */
(function () {
    'use strict';

    var currentResolve = null;
    var lastActiveElement = null;
    var isSubmitting = false;

    // Default icon mappings per variant
    var VARIANT_ICONS = {
        danger: 'bi bi-trash3-fill',
        warning: 'bi bi-exclamation-triangle-fill',
        primary: 'bi bi-check-circle-fill',
        success: 'bi bi-check-circle-fill',
        info: 'bi bi-info-circle-fill'
    };

    /**
     * Get or lazy-create the modal DOM element.
     */
    function getModalElement() {
        var modal = document.getElementById('jtConfirmModal');
        if (modal) return modal;

        // Fallback: create dynamically if template fragment was omitted
        var wrap = document.createElement('div');
        wrap.id = 'jtConfirmModal';
        wrap.className = 'jt-confirm-backdrop';
        wrap.setAttribute('role', 'dialog');
        wrap.setAttribute('aria-modal', 'true');
        wrap.setAttribute('aria-labelledby', 'jtConfirmTitle');
        wrap.setAttribute('aria-describedby', 'jtConfirmMessage');
        wrap.setAttribute('aria-hidden', 'true');
        wrap.style.display = 'none';

        wrap.innerHTML =
            '<div class="jt-confirm-card" role="document">' +
                '<button type="button" class="jt-confirm-close-btn" id="jtConfirmCloseBtn" aria-label="Close dialog" title="Close">' +
                    '<i class="bi bi-x-lg" aria-hidden="true"></i>' +
                '</button>' +
                '<div class="jt-confirm-icon-wrap" id="jtConfirmIconWrap" aria-hidden="true">' +
                    '<i id="jtConfirmIcon" class="bi bi-trash3-fill"></i>' +
                '</div>' +
                '<h2 class="jt-confirm-title" id="jtConfirmTitle">Confirm Action</h2>' +
                '<p class="jt-confirm-message" id="jtConfirmMessage">Are you sure you want to proceed?</p>' +
                '<div class="jt-confirm-actions" id="jtConfirmActions">' +
                    '<button type="button" class="jt-btn jt-confirm-cancel-btn" id="jtConfirmCancelBtn">Cancel</button>' +
                    '<button type="button" class="jt-btn jt-confirm-submit-btn" id="jtConfirmSubmitBtn">' +
                        '<span class="jt-confirm-btn-spinner d-none" aria-hidden="true"></span>' +
                        '<span class="jt-confirm-btn-text">Confirm</span>' +
                    '</button>' +
                '</div>' +
            '</div>';

        document.body.appendChild(wrap);
        return wrap;
    }

    /**
     * Reset button states (re-enable, hide spinner).
     */
    function resetButtonStates(modal) {
        isSubmitting = false;
        var submitBtn = modal.querySelector('#jtConfirmSubmitBtn');
        var cancelBtn = modal.querySelector('#jtConfirmCancelBtn');
        var closeBtn = modal.querySelector('#jtConfirmCloseBtn');
        var spinner = submitBtn ? submitBtn.querySelector('.jt-confirm-btn-spinner') : null;

        if (submitBtn) submitBtn.disabled = false;
        if (cancelBtn) cancelBtn.disabled = false;
        if (closeBtn) closeBtn.disabled = false;
        if (spinner) spinner.classList.add('d-none');
    }

    /**
     * Close the modal and return focus.
     */
    function closeModal(result) {
        var modal = getModalElement();
        modal.classList.remove('jt-confirm-open');
        modal.setAttribute('aria-hidden', 'true');
        document.body.classList.remove('jt-confirm-locked');

        // Short timeout for exit transition before hiding display
        setTimeout(function () {
            if (!modal.classList.contains('jt-confirm-open')) {
                modal.style.display = 'none';
                resetButtonStates(modal);
            }
        }, 150);

        if (currentResolve) {
            var res = currentResolve;
            currentResolve = null;
            res(Boolean(result));
        }

        if (lastActiveElement && typeof lastActiveElement.focus === 'function') {
            try {
                lastActiveElement.focus();
            } catch (e) {}
            lastActiveElement = null;
        }
    }

    /**
     * Programmatic confirm dialog returning a Promise<boolean>.
     *
     * @param {Object} options
     * @param {string} [options.title='Are you sure?']
     * @param {string} [options.message='Please confirm to proceed.']
     * @param {string} [options.confirmText]
     * @param {string} [options.cancelText='Cancel']
     * @param {string} [options.variant='danger'] - 'danger' | 'warning' | 'primary' | 'success' | 'info'
     * @param {string} [options.icon] - Bootstrap icon class (e.g. 'bi bi-trash3-fill')
     * @param {boolean} [options.isAlert=false]
     * @returns {Promise<boolean>}
     */
    function confirmDialog(options) {
        options = options || {};

        return new Promise(function (resolve) {
            // Dismiss any existing open modal
            if (currentResolve) {
                currentResolve(false);
            }
            currentResolve = resolve;

            try {
                lastActiveElement = document.activeElement;
            } catch (e) {
                lastActiveElement = null;
            }

            var modal = getModalElement();
            resetButtonStates(modal);

            var variant = options.variant || (options.isAlert ? 'warning' : 'danger');
            var isAlert = Boolean(options.isAlert);
            var title = options.title || (isAlert ? 'Notice' : 'Are you sure?');
            var message = options.message || '';
            var confirmText = options.confirmText || (isAlert ? 'OK' : (variant === 'danger' ? 'Delete' : 'Confirm'));
            var cancelText = options.cancelText || 'Cancel';
            var iconClass = options.icon || VARIANT_ICONS[variant] || VARIANT_ICONS.primary;

            // Reset variant classes
            modal.className = 'jt-confirm-backdrop';
            modal.classList.add('jt-variant-' + variant);
            if (isAlert) {
                modal.classList.add('jt-mode-alert');
            }

            // Populate content
            var titleEl = modal.querySelector('#jtConfirmTitle');
            var msgEl = modal.querySelector('#jtConfirmMessage');
            var submitBtn = modal.querySelector('#jtConfirmSubmitBtn');
            var submitText = submitBtn ? submitBtn.querySelector('.jt-confirm-btn-text') : null;
            var cancelBtn = modal.querySelector('#jtConfirmCancelBtn');
            var iconEl = modal.querySelector('#jtConfirmIcon');

            if (titleEl) titleEl.textContent = title;
            if (msgEl) msgEl.textContent = message;
            if (submitText) submitText.textContent = confirmText;
            if (cancelBtn) cancelBtn.textContent = cancelText;
            if (iconEl) iconEl.className = iconClass;

            // Display and animate
            modal.style.display = 'flex';
            // Force reflow for CSS animation
            void modal.offsetHeight;
            modal.classList.add('jt-confirm-open');
            modal.setAttribute('aria-hidden', 'false');
            document.body.classList.add('jt-confirm-locked');

            // Accessibility: Focus management
            // For danger variants, focus Cancel so accidental Enter doesn't trigger destructive actions.
            // For neutral/alert variants, focus Confirm.
            setTimeout(function () {
                if (variant === 'danger' && !isAlert && cancelBtn) {
                    cancelBtn.focus();
                } else if (submitBtn) {
                    submitBtn.focus();
                }
            }, 30);
        });
    }

    /**
     * Themed alert dialog replacing native alert().
     *
     * @param {string|Object} messageOrOptions
     * @returns {Promise<void>}
     */
    function alertDialog(messageOrOptions) {
        var opts = {};
        if (typeof messageOrOptions === 'string') {
            opts = {
                title: 'Notice',
                message: messageOrOptions,
                variant: 'warning',
                confirmText: 'OK',
                isAlert: true
            };
        } else {
            opts = Object.assign({}, messageOrOptions, { isAlert: true });
            if (!opts.confirmText) opts.confirmText = 'OK';
            if (!opts.title) opts.title = 'Notice';
        }
        return confirmDialog(opts).then(function () {});
    }

    /**
     * Submit action with double-submit prevention.
     */
    function handleConfirmSubmit() {
        if (isSubmitting) return;
        isSubmitting = true;

        var modal = getModalElement();
        var submitBtn = modal.querySelector('#jtConfirmSubmitBtn');
        var cancelBtn = modal.querySelector('#jtConfirmCancelBtn');
        var closeBtn = modal.querySelector('#jtConfirmCloseBtn');
        var spinner = submitBtn ? submitBtn.querySelector('.jt-confirm-btn-spinner') : null;

        if (submitBtn) submitBtn.disabled = true;
        if (cancelBtn) cancelBtn.disabled = true;
        if (closeBtn) closeBtn.disabled = true;
        if (spinner) spinner.classList.remove('d-none');

        closeModal(true);
    }

    /**
     * Initialize modal event listeners once on DOM ready.
     */
    function initModalEvents() {
        var modal = getModalElement();

        // Click on backdrop closes
        modal.addEventListener('click', function (e) {
            if (e.target === modal) {
                closeModal(false);
            }
        });

        // Close button click
        var closeBtn = modal.querySelector('#jtConfirmCloseBtn');
        if (closeBtn) {
            closeBtn.addEventListener('click', function (e) {
                e.preventDefault();
                closeModal(false);
            });
        }

        // Cancel button click
        var cancelBtn = modal.querySelector('#jtConfirmCancelBtn');
        if (cancelBtn) {
            cancelBtn.addEventListener('click', function (e) {
                e.preventDefault();
                closeModal(false);
            });
        }

        // Submit button click
        var submitBtn = modal.querySelector('#jtConfirmSubmitBtn');
        if (submitBtn) {
            submitBtn.addEventListener('click', function (e) {
                e.preventDefault();
                handleConfirmSubmit();
            });
        }

        // Keyboard navigation and Tab trap
        document.addEventListener('keydown', function (e) {
            if (!modal.classList.contains('jt-confirm-open')) return;

            if (e.key === 'Escape') {
                e.preventDefault();
                closeModal(false);
                return;
            }

            if (e.key === 'Tab') {
                var focusables = Array.from(modal.querySelectorAll('button:not([disabled]):not([tabindex="-1"])'))
                    .filter(function (el) {
                        return el.offsetWidth > 0 || el.offsetHeight > 0 || el.getClientRects().length > 0;
                    });

                if (focusables.length === 0) return;

                var first = focusables[0];
                var last = focusables[focusables.length - 1];

                if (e.shiftKey && document.activeElement === first) {
                    e.preventDefault();
                    last.focus();
                } else if (!e.shiftKey && document.activeElement === last) {
                    e.preventDefault();
                    first.focus();
                }
            }
        });
    }

    /**
     * Read configuration from declarative data-confirm attributes.
     */
    function getDeclarativeConfig(target) {
        var form = target.tagName === 'FORM' ? target : target.closest('form');
        var source = target;

        function readAttr(name) {
            return source.getAttribute(name) || (form ? form.getAttribute(name) : null);
        }

        var isDeleteAction = false;
        var btnText = (source.textContent || '').trim().toLowerCase();
        var formAction = (form ? form.getAttribute('action') || '' : '').toLowerCase();
        if (btnText.indexOf('delete') !== -1 || formAction.indexOf('delete') !== -1) {
            isDeleteAction = true;
        }

        var variant = readAttr('data-confirm-variant') || (isDeleteAction ? 'danger' : 'primary');
        var title = readAttr('data-confirm-title') || (isDeleteAction ? 'Delete item?' : 'Confirm action?');
        var message = readAttr('data-confirm-message') || (readAttr('data-confirm') !== 'true' ? readAttr('data-confirm') : 'Are you sure you want to proceed?');
        var confirmButton = readAttr('data-confirm-button') || (variant === 'danger' ? 'Delete' : 'Confirm');
        var cancelButton = readAttr('data-confirm-cancel') || 'Cancel';
        var icon = readAttr('data-confirm-icon');

        return {
            title: title,
            message: message,
            confirmText: confirmButton,
            cancelText: cancelButton,
            variant: variant,
            icon: icon
        };
    }

    /**
     * Document-level event delegation for declarative usage:
     * intercepts clicks on buttons/links and submits on forms with [data-confirm].
     */
    function initDelegatedTriggers() {
        // Form submit delegation
        document.addEventListener('submit', function (e) {
            var form = e.target.closest('form[data-confirm]');
            if (!form) return;

            // If already confirmed, allow natural submit
            if (form.__jtConfirmed) {
                delete form.__jtConfirmed;
                return;
            }

            e.preventDefault();
            e.stopImmediatePropagation();

            var config = getDeclarativeConfig(form);
            confirmDialog(config).then(function (confirmed) {
                if (confirmed) {
                    form.__jtConfirmed = true;
                    HTMLFormElement.prototype.submit.call(form);
                }
            });
        }, true);

        // Click delegation on buttons / links
        document.addEventListener('click', function (e) {
            var trigger = e.target.closest('[data-confirm]');
            if (!trigger) return;

            // If it's a form, submit listener will handle it
            if (trigger.tagName === 'FORM') return;

            // If trigger is a submit button inside a form with data-confirm, let submit listener handle it
            var form = trigger.closest('form');
            if (trigger.type === 'submit' && form && form.hasAttribute('data-confirm')) {
                return;
            }

            if (trigger.__jtConfirmed) {
                delete trigger.__jtConfirmed;
                return;
            }

            e.preventDefault();
            e.stopImmediatePropagation();

            var config = getDeclarativeConfig(trigger);
            confirmDialog(config).then(function (confirmed) {
                if (!confirmed) return;

                trigger.__jtConfirmed = true;
                if (trigger.tagName === 'A' && trigger.href) {
                    window.location.href = trigger.href;
                } else if (form) {
                    form.__jtConfirmed = true;
                    HTMLFormElement.prototype.submit.call(form);
                } else {
                    // Regular button with custom click logic
                    trigger.click();
                }
            });
        }, true);
    }

    // Initialize on DOM load
    document.addEventListener('DOMContentLoaded', function () {
        initModalEvents();
        initDelegatedTriggers();
    });

    // Expose Global Public APIs
    window.confirmDialog = confirmDialog;
    window.alertDialog = alertDialog;

    // Wrap native alert() so legacy calls display the themed modal
    window.__nativeAlert = window.alert;
    window.alert = function (msg) {
        return alertDialog(msg);
    };

})();
