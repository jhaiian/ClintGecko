(function() {
    if (window.top !== window) return;
    if (window.__clintDateTimePickerInit) return;
    window.__clintDateTimePickerInit = true;

    var TYPES = ['date', 'time', 'datetime-local', 'month', 'week'];
    var nextId = 1;
    var current = null;
    var lastOpenAt = 0;

    function isPickerInput(el) {
        return !!el && el.tagName === 'INPUT' && TYPES.indexOf(el.type) !== -1 && !el.disabled && !el.readOnly;
    }

    function openPicker(input) {
        var now = Date.now();
        if (current && current.input === input && now - lastOpenAt < 500) return;
        lastOpenAt = now;
        current = { id: String(nextId++), input: input };
        DateTimePickerBridge.onDateTimeOpen(current.id, input.type, input.value || '');
    }

    function intercept(e) {
        var target = e.target;
        if (!isPickerInput(target)) return;
        e.preventDefault();
        e.stopPropagation();
        target.blur();
        openPicker(target);
    }

    document.addEventListener('mousedown', intercept, true);
    document.addEventListener('click', intercept, true);

    var proto = window.HTMLInputElement && window.HTMLInputElement.prototype;
    if (proto && typeof proto.showPicker === 'function') {
        var nativeShowPicker = proto.showPicker;
        proto.showPicker = function() {
            if (isPickerInput(this)) {
                openPicker(this);
                return;
            }
            return nativeShowPicker.apply(this, arguments);
        };
    }

    function clamp(value, min, max) {
        if (min && max && min > max) return value;
        if (min && value < min) return min;
        if (max && value > max) return max;
        return value;
    }

    window.__clintApplyDateTime = function(id, value) {
        if (!current || current.id !== id) return;
        var input = current.input;
        current = null;
        var next = clamp(value, input.min, input.max);
        var setter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value').set;
        if (setter) {
            setter.call(input, next);
        } else {
            input.value = next;
        }
        input.dispatchEvent(new Event('input', { bubbles: true }));
        input.dispatchEvent(new Event('change', { bubbles: true }));
    };
})();
