(function() {
    if (window.top !== window) return;
    if (window.__clintColorPickerInit) return;
    window.__clintColorPickerInit = true;

    var nextId = 1;
    var current = null;
    var lastOpenAt = 0;

    function isColorInput(el) {
        return !!el && el.tagName === 'INPUT' && el.type === 'color' && !el.disabled;
    }

    function openPicker(input) {
        var now = Date.now();
        if (current && current.input === input && now - lastOpenAt < 500) return;
        lastOpenAt = now;
        current = { id: String(nextId++), input: input };
        ColorPickerBridge.onColorOpen(current.id, input.value || '#000000');
    }

    function intercept(e) {
        var target = e.target;
        if (!isColorInput(target)) return;
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
            if (isColorInput(this)) {
                openPicker(this);
                return;
            }
            return nativeShowPicker.apply(this, arguments);
        };
    }

    window.__clintApplyColor = function(id, value) {
        if (!current || current.id !== id) return;
        var input = current.input;
        current = null;
        var before = input.value;
        var setter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value').set;
        if (setter) {
            setter.call(input, value);
        } else {
            input.value = value;
        }
        if (input.value === before) return;
        input.dispatchEvent(new Event('input', { bubbles: true }));
        input.dispatchEvent(new Event('change', { bubbles: true }));
    };
})();
