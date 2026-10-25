(function() {
    var C = window.LONG_SHOT || {};
    var S = C.strings || {};
    var root = document.getElementById('ls');
    var canvas = document.getElementById('ls-canvas');
    if (!root || !canvas || !canvas.getContext) return;
    root.hidden = false;

    var ctx = canvas.getContext('2d');
    var startEl = document.getElementById('ls-start');
    var overEl = document.getElementById('ls-over');
    var live = document.getElementById('ls-live');
    var soundBtn = document.getElementById('ls-sound');
    var reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)');
    var MAX_MISSES = 3;
    var STEP = 1 / 120;
    var BULL = 0.22;
    var INNER = 0.6;
    var WW = 100;
    var W = 0, H = 0, dpr = 1, k = 1, ox = 0, oy = 0, px = 1;
    var WH = 62.5, GROUND = 56;
    var colors = {};
    var font = 'sans-serif';
    var best = Math.max(0, C.best | 0);
    var soundOn = C.sound !== false;
    var audio = null, master = null, noiseBuf = null;
    var g;
    var decimal = null;
    try {
        decimal = new Intl.NumberFormat(C.lang || undefined, { minimumFractionDigits: 1, maximumFractionDigits: 1 });
    } catch (e) {}

    function fmt(s, a, b) {
        return String(s || '').replace('%1$d', a).replace('%2$d', b).replace('%d', a);
    }

    function upper(s) {
        try { return String(s || '').toLocaleUpperCase(C.lang || undefined); } catch (e) { return String(s || '').toUpperCase(); }
    }

    function oneDecimal(v) {
        return decimal ? decimal.format(v) : v.toFixed(1);
    }

    var pageTitle = document.title;
    var titleTimer = 0, saveTimer = 0;

    function persist() {
        if (!C.save) return;
        clearTimeout(titleTimer);
        document.title = C.save + '?best=' + best + '&sound=' + (soundOn ? 1 : 0);
        titleTimer = setTimeout(function() { document.title = pageTitle; }, 400);
    }

    function queuePersist() {
        if (saveTimer) return;
        saveTimer = setTimeout(function() {
            saveTimer = 0;
            persist();
        }, 600);
    }

    function readColors() {
        var cs = getComputedStyle(document.documentElement);
        ['fg', 'fg2', 'primary', 'tonal', 'divider', 'error', 'field', 'gold', 'bg'].forEach(function(k) {
            colors[k] = cs.getPropertyValue('--' + k).trim();
        });
        font = getComputedStyle(document.body).fontFamily || 'sans-serif';
    }

    function updateSoundButton() {
        if (!soundBtn) return;
        soundBtn.setAttribute('aria-pressed', soundOn ? 'true' : 'false');
        soundBtn.title = soundOn ? S.mute : S.unmute;
    }

    function toggleSound() {
        soundOn = !soundOn;
        updateSoundButton();
        if (soundOn) {
            ensureAudio();
            tone(880, 880, 0.09, 'sine', 0.12);
        }
        persist();
    }

    function ensureAudio() {
        if (!soundOn) return;
        if (!audio) {
            var AC = window.AudioContext || window.webkitAudioContext;
            if (!AC) return;
            try { audio = new AC(); } catch (e) { return; }
            master = audio.createGain();
            master.gain.value = 0.6;
            master.connect(audio.destination);
            noiseBuf = audio.createBuffer(1, Math.floor(audio.sampleRate * 0.8), audio.sampleRate);
            var d = noiseBuf.getChannelData(0);
            for (var i = 0; i < d.length; i++) d[i] = Math.random() * 2 - 1;
        }
        if (audio.state === 'suspended') audio.resume();
    }

    function envelope(gain, t0, vol, dur) {
        gain.gain.setValueAtTime(0.0001, t0);
        gain.gain.exponentialRampToValueAtTime(vol, t0 + 0.008);
        gain.gain.exponentialRampToValueAtTime(0.0001, t0 + dur);
    }

    function tone(f0, f1, dur, type, vol, delay) {
        if (!soundOn || !audio) return;
        var t0 = audio.currentTime + (delay || 0);
        var o = audio.createOscillator(), gn = audio.createGain();
        o.type = type;
        o.frequency.setValueAtTime(f0, t0);
        if (f1 !== f0) o.frequency.exponentialRampToValueAtTime(f1, t0 + dur);
        envelope(gn, t0, vol, dur);
        o.connect(gn);
        gn.connect(master);
        o.start(t0);
        o.stop(t0 + dur + 0.03);
    }

    function hiss(dur, kind, f0, f1, vol, delay, q) {
        if (!soundOn || !audio) return;
        var t0 = audio.currentTime + (delay || 0);
        var src = audio.createBufferSource(), flt = audio.createBiquadFilter(), gn = audio.createGain();
        src.buffer = noiseBuf;
        flt.type = kind;
        flt.Q.value = q || 1;
        flt.frequency.setValueAtTime(f0, t0);
        if (f1 !== f0) flt.frequency.exponentialRampToValueAtTime(f1, t0 + dur);
        envelope(gn, t0, vol, dur);
        src.connect(flt);
        flt.connect(gn);
        gn.connect(master);
        src.start(t0, Math.random() * 0.25);
        src.stop(t0 + dur + 0.03);
    }

    var sfx = {
        draw: function() {
            ensureAudio();
            hiss(0.3, 'bandpass', 650, 1150, 0.06, 0, 7);
        },
        release: function(power) {
            var f = 150 + power * 110;
            tone(f, f * 0.55, 0.2, 'triangle', 0.32);
            tone(f * 2.01, f * 1.1, 0.12, 'sine', 0.08);
            hiss(0.05, 'highpass', 3000, 3000, 0.12);
            hiss(0.45, 'bandpass', 1800, 420, 0.05 + power * 0.07, 0.03, 1.4);
        },
        hit: function(bull, mult) {
            tone(150, 60, 0.14, 'sine', 0.55);
            hiss(0.07, 'lowpass', 1400, 500, 0.3);
            if (bull) {
                var p = Math.pow(1.122, Math.max(0, mult - 1));
                tone(1318 * p, 1318 * p, 0.7, 'sine', 0.16, 0.05);
                tone(1976 * p, 1976 * p, 0.5, 'sine', 0.07, 0.05);
            }
        },
        miss: function(inGround) {
            if (inGround) {
                tone(95, 45, 0.12, 'sine', 0.35);
                hiss(0.1, 'lowpass', 500, 200, 0.2);
            }
            tone(320, 210, 0.18, 'triangle', 0.1, 0.06);
        },
        end: function(record) {
            var notes = record ? [523, 659, 784, 1047] : [392, 330, 262];
            notes.forEach(function(f, i) {
                tone(f, f, record ? 0.22 : 0.26, 'triangle', 0.18, i * (record ? 0.11 : 0.16));
            });
        }
    };

    function fresh() {
        return {
            mode: 'ready', time: 0, score: 0, misses: 0, hits: 0, streak: 0,
            wind: 0, flying: null, stuck: [], ground: [], popups: [],
            target: { x: 58, h: 22, amp: 0, phase: 0 },
            aim: null, keyAngle: -0.32, keyPower: 0, charging: false,
            lockedUntil: 0, relocateAt: 0, overAt: 0, wobble: 0
        };
    }

    function resize() {
        var r = canvas.getBoundingClientRect();
        W = r.width;
        H = r.height;
        dpr = Math.min(window.devicePixelRatio || 1, 2);
        canvas.width = Math.max(1, Math.round(W * dpr));
        canvas.height = Math.max(1, Math.round(H * dpr));
        WH = Math.max(62.5, Math.min(90, W > 0 ? 100 * H / W : 62.5));
        GROUND = WH - 6.5;
        k = Math.max(0.01, Math.min(W / WW, H / WH, 7));
        ox = (W - WW * k) / 2;
        oy = (H - WH * k) / 2;
        px = 1 / k;
    }

    function worldTransform() { ctx.setTransform(dpr * k, 0, 0, dpr * k, dpr * ox, dpr * oy); }
    function screenTransform() { ctx.setTransform(dpr, 0, 0, dpr, 0, 0); }
    function toScreenX(x) { return ox + x * k; }
    function toScreenY(y) { return oy + y * k; }
    function leftEdge() { return -ox / k; }
    function rightEdge() { return (W - ox) / k; }
    function topEdge() { return -oy / k; }

    var GRAVITY = 62, MAX_SPEED = 104, WIND_ACCEL = 20, ARROW = 6.5;
    function radius() { return Math.max(4.6, 9.2 - g.hits * 0.26); }
    function launch() { return { x: 13.2, y: GROUND - 10.5 }; }

    function targetPos() {
        var t = g.target;
        var len = GROUND - t.h;
        var theta = t.amp * Math.sin(t.phase);
        return { x: t.x + Math.sin(theta) * len, y: Math.cos(theta) * len, ax: t.x, theta: theta };
    }

    function toLocal(x, y, tp) {
        var dx = x - tp.x, dy = y - tp.y;
        var c = Math.cos(-tp.theta), s = Math.sin(-tp.theta);
        return { x: dx * c - dy * s, y: dx * s + dy * c };
    }

    function place(dx, height) {
        var lp = launch();
        var R = radius();
        var x = Math.min(lp.x + dx, WW - R - 4);
        var y = Math.max(R + 10, Math.min(GROUND - R - 4, lp.y - height));
        g.target.x = x;
        g.target.h = GROUND - y;
    }

    function relocate() {
        var maxHeight = 42 + (WH - 62.5) * 0.3;
        place(32 + Math.random() * 46, -8 + Math.random() * (maxHeight + 8));
        g.target.amp = g.hits < 4 ? 0 : Math.min(0.34, (g.hits - 3) * 0.045);
        g.target.phase = Math.random() * Math.PI * 2;
        g.stuck = [];
        newWind();
    }

    function newWind() {
        var max = Math.min(1, 0.15 + g.hits * 0.07);
        g.wind = g.hits === 0 && g.misses === 0 ? 0 : (Math.random() * 2 - 1) * max;
    }

    function say(text) { if (live) live.textContent = text; }

    function start() {
        g = fresh();
        g.mode = 'playing';
        g.startBest = best;
        place(45, 12);
        ensureAudio();
        startEl.hidden = true;
        overEl.hidden = true;
        try { canvas.focus({ preventScroll: true }); } catch (e) {}
        if (!C.full && root.scrollIntoView) {
            try { root.scrollIntoView({ block: 'nearest' }); } catch (e) {}
        }
        say(S.started);
    }

    function canShoot() {
        return g.mode === 'playing' && !g.flying && g.time >= g.lockedUntil && !g.relocateAt && !g.overAt;
    }

    function aimFromPull(dx, dy) {
        var maxPull = Math.min(W, H) * 0.42;
        var dist = Math.hypot(dx, dy);
        var angle = Math.atan2(dy, dx);
        if (angle > 2.2 || angle < -2.2) angle = angle > 0 ? 0.9 : -1.4;
        angle = Math.max(-1.4, Math.min(0.9, angle));
        return { angle: angle, power: Math.min(dist / maxPull, 1) };
    }

    function currentAim() {
        if (g.aim && g.aim.cur) return aimFromPull(g.aim.sx - g.aim.cur.x, g.aim.sy - g.aim.cur.y);
        if (g.charging) return { angle: g.keyAngle, power: g.keyPower };
        return null;
    }

    function fire(a) {
        if (!a || a.power < 0.12 || !canShoot()) return;
        var p = launch();
        var v = MAX_SPEED * (0.25 + 0.75 * a.power);
        g.flying = { x: p.x, y: p.y, vx: Math.cos(a.angle) * v, vy: Math.sin(a.angle) * v };
        sfx.release(a.power);
    }

    function pointer(e) {
        var r = canvas.getBoundingClientRect();
        return { x: e.clientX - r.left, y: e.clientY - r.top };
    }

    canvas.addEventListener('pointerdown', function(e) {
        if (g.mode !== 'playing' || !canShoot()) return;
        var p = pointer(e);
        try { canvas.setPointerCapture(e.pointerId); } catch (err) {}
        g.aim = { id: e.pointerId, sx: p.x, sy: p.y, cur: p };
        sfx.draw();
        e.preventDefault();
    });
    canvas.addEventListener('pointermove', function(e) {
        if (!g.aim || g.aim.id !== e.pointerId) return;
        g.aim.cur = pointer(e);
    });

    function endPointer(e, shoot) {
        if (!g.aim || g.aim.id !== e.pointerId) return;
        var a = currentAim();
        g.aim = null;
        if (shoot) fire(a);
    }

    canvas.addEventListener('pointerup', function(e) { endPointer(e, true); });
    canvas.addEventListener('pointercancel', function(e) { endPointer(e, false); });

    function isControl(el) {
        var tag = el && el.tagName;
        return tag === 'BUTTON' || tag === 'A' || tag === 'SUMMARY' || tag === 'INPUT' || tag === 'TEXTAREA';
    }

    window.addEventListener('keydown', function(e) {
        if (e.ctrlKey || e.metaKey || e.altKey) return;
        if (e.key === ' ' || e.code === 'Space') {
            if (isControl(e.target)) return;
            e.preventDefault();
            if (g.mode !== 'playing') {
                if (!e.repeat) start();
                return;
            }
            if (!g.charging && canShoot() && !e.repeat) {
                g.charging = true;
                g.keyPower = 0;
                sfx.draw();
            }
        } else if ((e.key === 'm' || e.key === 'M') && !e.repeat && !isControl(e.target)) {
            toggleSound();
        } else if (g.mode === 'playing' && (e.key === 'ArrowUp' || e.key === 'ArrowDown')) {
            e.preventDefault();
            g.keyAngle = Math.max(-1.4, Math.min(0.9, g.keyAngle + (e.key === 'ArrowUp' ? -0.04 : 0.04)));
        }
    });
    window.addEventListener('keyup', function(e) {
        if ((e.key === ' ' || e.code === 'Space') && g.charging) {
            g.charging = false;
            fire({ angle: g.keyAngle, power: g.keyPower });
            g.keyPower = 0;
        }
    });

    document.getElementById('ls-play').addEventListener('click', start);
    document.getElementById('ls-again').addEventListener('click', start);
    if (soundBtn) soundBtn.addEventListener('click', toggleSound);

    function popup(x, y, lift, text, color) {
        g.popups.push({ x: x, y: y, lift: lift, text: text, color: color, born: g.time });
    }

    function hit(tp, ly, angle) {
        var R = radius();
        var ratio = Math.abs(ly) / R;
        var bull = ratio <= BULL;
        var base = bull ? 5 : ratio <= INNER ? 3 : 1;
        g.streak = bull ? g.streak + 1 : 0;
        var mult = bull ? Math.min(g.streak, 3) : 1;
        var pts = base * mult;
        g.score += pts;
        g.hits += 1;
        if (g.score > best) {
            best = g.score;
            queuePersist();
        }
        g.stuck.push({ oy: ly, angle: angle - tp.theta });
        g.wobble = 1;
        sfx.hit(bull, mult);
        popup(tp.x, tp.y - R - 2, 0, '+' + pts, bull ? colors.gold : colors.primary);
        if (bull) popup(tp.x, tp.y - R - 2, -24, mult > 1 ? fmt(S.bullseyeMultiplier, mult) : S.bullseye, colors.gold);
        say(fmt(bull ? S.liveBullseye : S.liveHit, pts, g.score));
        g.relocateAt = g.time + 0.75;
    }

    function miss(x, y, inGround, angle) {
        g.misses += 1;
        g.streak = 0;
        if (inGround) g.ground.push({ x: x, y: y, angle: angle, born: g.time });
        sfx.miss(inGround);
        var left = MAX_MISSES - g.misses;
        popup(Math.max(leftEdge() + 8, Math.min(rightEdge() - 8, x)), Math.min(y, GROUND) - 4, 0, S.miss, colors.error);
        if (left <= 0) {
            g.overAt = g.time + 0.9;
            say(fmt(S.liveOver, g.score));
        } else {
            say(fmt(left === 1 ? S.liveMissOne : S.liveMissMany, left));
            newWind();
            g.lockedUntil = g.time + 0.35;
        }
    }

    function stepFlight(dt, before) {
        var a = g.flying;
        if (!a) return;
        var px0 = a.x, py = a.y;
        a.vy += GRAVITY * dt;
        a.vx += g.wind * WIND_ACCEL * dt;
        a.x += a.vx * dt;
        a.y += a.vy * dt;
        var angle = Math.atan2(a.vy, a.vx);
        var tp = targetPos();
        var R = radius();
        var lp = toLocal(px0, py, before), lc = toLocal(a.x, a.y, tp);
        if (lp.x < 0 && lc.x >= 0) {
            var k = -lp.x / (lc.x - lp.x || 1);
            var ly = lp.y + (lc.y - lp.y) * k;
            if (Math.abs(ly) <= R) {
                g.flying = null;
                hit(tp, ly, angle);
                return;
            }
        }
        if (a.y >= GROUND) {
            var f = (GROUND - py) / (a.y - py || 1);
            g.flying = null;
            miss(px0 + (a.x - px0) * f, GROUND, true, angle);
            return;
        }
        if (a.x > rightEdge() + 12 || a.x < leftEdge() - 12) {
            g.flying = null;
            miss(a.x, Math.max(topEdge() + 8, a.y), false, angle);
        }
    }

    function update(dt) {
        g.time += dt;
        var t = g.target;
        var before = targetPos();
        if (t.amp > 0) t.phase += dt * 1.7;
        if (g.wobble > 0) g.wobble = Math.max(0, g.wobble - dt * 2.5);
        if (g.charging) g.keyPower = Math.min(1, g.keyPower + dt / 1.1);
        if (g.relocateAt && g.time >= g.relocateAt) {
            g.relocateAt = 0;
            relocate();
        }
        if (g.overAt && g.time >= g.overAt) {
            g.overAt = 0;
            gameOver();
        }
        stepFlight(dt, before);
        g.popups = g.popups.filter(function(p) { return g.time - p.born < 1.1; });
        g.ground = g.ground.filter(function(p) { return g.time - p.born < 2.4; });
    }

    function gameOver() {
        g.mode = 'over';
        g.aim = null;
        g.charging = false;
        var record = g.score > g.startBest;
        sfx.end(record);
        document.getElementById('ls-over-title').textContent = record ? S.newBest : S.gameOver;
        document.getElementById('ls-final').textContent = g.score;
        document.getElementById('ls-best').textContent = best;
        overEl.hidden = false;
        try { document.getElementById('ls-again').focus({ preventScroll: true }); } catch (e) {}
    }

    function drawArrow(x, y, angle, len, alpha) {
        ctx.save();
        ctx.globalAlpha = alpha == null ? 1 : alpha;
        ctx.translate(x, y);
        ctx.rotate(angle);
        ctx.strokeStyle = colors.fg;
        ctx.lineWidth = Math.max(1.5 * px, 0.35);
        ctx.lineCap = 'round';
        ctx.beginPath(); ctx.moveTo(-len, 0); ctx.lineTo(0, 0); ctx.stroke();
        ctx.fillStyle = colors.fg;
        ctx.beginPath(); ctx.moveTo(0.9, 0); ctx.lineTo(-0.9, -0.7); ctx.lineTo(-0.9, 0.7); ctx.closePath(); ctx.fill();
        ctx.fillStyle = colors.primary;
        ctx.beginPath(); ctx.moveTo(-len, 0); ctx.lineTo(-len - 1.2, -0.9); ctx.lineTo(-len + 1.4, 0); ctx.lineTo(-len - 1.2, 0.9); ctx.closePath(); ctx.fill();
        ctx.restore();
    }

    function drawTarget() {
        var tp = targetPos();
        var R = radius() * (1 + (reduceMotion.matches ? 0 : Math.sin(g.wobble * 14) * g.wobble * 0.05));
        var sin = Math.sin(tp.theta), cos = Math.cos(tp.theta);
        var top = topEdge();
        var reach = top / (cos || 1);
        ctx.strokeStyle = colors.fg2;
        ctx.lineWidth = 1.5 * px;
        ctx.beginPath();
        ctx.moveTo(tp.ax + sin * reach, top);
        ctx.lineTo(tp.x - sin * R, tp.y - cos * R);
        ctx.stroke();
        var rx = Math.max(5 * px, R * 0.34);
        var rings = [[1, colors.tonal], [INNER, colors.primary], [BULL, colors.error]];
        ctx.save();
        ctx.translate(tp.x, tp.y);
        ctx.rotate(tp.theta);
        ctx.fillStyle = colors.fg2;
        ctx.beginPath(); ctx.ellipse(rx * 0.35, 0, rx, R, 0, 0, Math.PI * 2); ctx.fill();
        rings.forEach(function(r) {
            ctx.fillStyle = r[1];
            ctx.beginPath(); ctx.ellipse(0, 0, rx * r[0], R * r[0], 0, 0, Math.PI * 2); ctx.fill();
        });
        ctx.strokeStyle = colors.fg;
        ctx.globalAlpha = 0.35;
        ctx.lineWidth = px;
        ctx.beginPath(); ctx.ellipse(0, 0, rx, R, 0, 0, Math.PI * 2); ctx.stroke();
        ctx.globalAlpha = 1;
        g.stuck.forEach(function(a) { drawArrow(rx * 0.1, a.oy, a.angle, ARROW); });
        ctx.restore();
    }

    function drawArcher(aim) {
        var fx = 10;
        var hipY = GROUND - 6;
        var shoulderY = GROUND - 10.5;
        ctx.strokeStyle = colors.fg;
        ctx.fillStyle = colors.fg;
        ctx.lineWidth = Math.max(2 * px, 0.55);
        ctx.lineCap = 'round';
        ctx.beginPath();
        ctx.moveTo(fx - 2, GROUND); ctx.lineTo(fx, hipY); ctx.lineTo(fx + 2, GROUND);
        ctx.moveTo(fx, hipY); ctx.lineTo(fx, shoulderY - 0.5);
        ctx.stroke();
        ctx.beginPath(); ctx.arc(fx, shoulderY - 3, 1.8, 0, Math.PI * 2); ctx.fill();

        var angle = aim ? aim.angle : (g.charging ? g.keyAngle : -0.25);
        var power = aim ? aim.power : 0;
        var p = launch();
        var bowR = 4.6;
        var cos = Math.cos(angle), sin = Math.sin(angle);
        var topX = p.x + Math.cos(angle - 1.25) * bowR, topY = p.y + Math.sin(angle - 1.25) * bowR;
        var botX = p.x + Math.cos(angle + 1.25) * bowR, botY = p.y + Math.sin(angle + 1.25) * bowR;
        var pull = 1.6 + power * 4.2;
        var nockX = p.x - cos * pull, nockY = p.y - sin * pull;

        ctx.beginPath(); ctx.moveTo(fx, shoulderY); ctx.lineTo(p.x, p.y); ctx.stroke();
        ctx.beginPath(); ctx.moveTo(fx, shoulderY); ctx.lineTo(nockX, nockY); ctx.stroke();

        ctx.strokeStyle = colors.primary;
        ctx.lineWidth = Math.max(2.5 * px, 0.7);
        ctx.beginPath(); ctx.arc(p.x - cos * bowR * 0.3, p.y - sin * bowR * 0.3, bowR, angle - 1.25, angle + 1.25); ctx.stroke();
        ctx.strokeStyle = colors.fg2;
        ctx.lineWidth = px;
        ctx.beginPath();
        ctx.moveTo(topX - cos * bowR * 0.3, topY - sin * bowR * 0.3);
        ctx.lineTo(nockX, nockY);
        ctx.lineTo(botX - cos * bowR * 0.3, botY - sin * bowR * 0.3);
        ctx.stroke();

        if (!g.flying && g.mode === 'playing' && !g.relocateAt) {
            drawArrow(nockX + cos * ARROW, nockY + sin * ARROW, angle, ARROW);
        }
    }

    function drawPreview(aim) {
        if (!aim || aim.power < 0.12) return;
        var p = launch();
        var v = MAX_SPEED * (0.25 + 0.75 * aim.power);
        var vx = Math.cos(aim.angle) * v, vy = Math.sin(aim.angle) * v;
        var x = p.x, y = p.y;
        ctx.fillStyle = colors.fg2;
        for (var i = 1; i <= 9; i++) {
            var t = 0.045;
            vy += GRAVITY * t;
            x += vx * t;
            y += vy * t;
            ctx.globalAlpha = 0.75 - i * 0.07;
            ctx.beginPath(); ctx.arc(x, y, Math.max(1.5 * px, 0.45), 0, Math.PI * 2); ctx.fill();
        }
        ctx.globalAlpha = 1;
    }

    function drawHud() {
        var s = Math.max(1, Math.min(k, 6));
        var pad = 14;
        ctx.textBaseline = 'top';
        ctx.textAlign = 'left';
        ctx.font = '600 ' + Math.round(14 + s * 1.6) + 'px ' + font;
        ctx.fillStyle = colors.fg;
        ctx.fillText(String(g.score), pad, pad);
        ctx.font = '500 11px ' + font;
        ctx.fillStyle = colors.fg2;
        ctx.fillText(upper(S.best) + ' ' + best, pad, pad + 18 + s * 1.8);

        var r = 5.5;
        for (var i = 0; i < MAX_MISSES; i++) {
            var cx = W - pad - r - i * (r * 2 + 7);
            ctx.beginPath(); ctx.arc(cx, pad + r + 2, r, 0, Math.PI * 2);
            if (i < g.misses) {
                ctx.strokeStyle = colors.error;
                ctx.lineWidth = 1.5;
                ctx.stroke();
            } else {
                ctx.fillStyle = colors.primary;
                ctx.fill();
            }
        }

        var wx = W / 2, wy = pad + 2;
        var strength = Math.abs(g.wind);
        ctx.textAlign = 'center';
        ctx.fillStyle = colors.fg2;
        ctx.fillText(strength < 0.05 ? upper(S.noWind) : upper(S.wind) + ' ' + oneDecimal(strength * 10), wx, wy);
        if (strength >= 0.05) {
            var dir = g.wind > 0 ? 1 : -1;
            var len = 10 + strength * 34;
            var ay = wy + 22;
            ctx.strokeStyle = colors.primary;
            ctx.fillStyle = colors.primary;
            ctx.lineWidth = 2.5;
            ctx.lineCap = 'round';
            ctx.beginPath(); ctx.moveTo(wx - dir * len / 2, ay); ctx.lineTo(wx + dir * len / 2, ay); ctx.stroke();
            ctx.beginPath();
            ctx.moveTo(wx + dir * (len / 2 + 6), ay);
            ctx.lineTo(wx + dir * (len / 2 - 2), ay - 5);
            ctx.lineTo(wx + dir * (len / 2 - 2), ay + 5);
            ctx.closePath();
            ctx.fill();
        }
    }

    function draw() {
        screenTransform();
        ctx.clearRect(0, 0, W, H);
        var gy = Math.round(toScreenY(GROUND));
        ctx.fillStyle = colors.tonal;
        ctx.fillRect(0, gy, W, H - gy);
        ctx.strokeStyle = colors.divider;
        ctx.lineWidth = 1;
        ctx.beginPath(); ctx.moveTo(0, gy + 0.5); ctx.lineTo(W, gy + 0.5); ctx.stroke();

        worldTransform();
        drawTarget();
        g.ground.forEach(function(a) {
            var age = g.time - a.born;
            drawArrow(a.x, a.y, a.angle, ARROW, age > 1.6 ? Math.max(0, 1 - (age - 1.6) / 0.8) : 1);
        });
        var aim = g.mode === 'playing' && canShoot() ? currentAim() : null;
        drawPreview(aim);
        drawArcher(aim);
        var f = g.flying;
        if (f && toScreenY(f.y) >= -4) drawArrow(f.x, f.y, Math.atan2(f.vy, f.vx), ARROW);

        screenTransform();
        if (f && toScreenY(f.y) < -4) {
            var sx = toScreenX(f.x);
            ctx.fillStyle = colors.primary;
            ctx.beginPath(); ctx.moveTo(sx, 4); ctx.lineTo(sx - 6, 14); ctx.lineTo(sx + 6, 14); ctx.closePath(); ctx.fill();
        }
        if (g.mode !== 'ready') drawHud();

        ctx.textAlign = 'center';
        ctx.textBaseline = 'middle';
        ctx.font = '700 ' + Math.round(15 + Math.min(k, 6)) + 'px ' + font;
        g.popups.forEach(function(p) {
            var age = g.time - p.born;
            ctx.globalAlpha = Math.max(0, 1 - age / 1.1);
            ctx.fillStyle = p.color;
            ctx.fillText(p.text, toScreenX(p.x), toScreenY(p.y) - 10 + p.lift - (reduceMotion.matches ? 0 : age * 26));
        });
        ctx.globalAlpha = 1;
    }

    var last = 0, acc = 0;
    function frame(now) {
        var dt = last ? Math.min(0.05, (now - last) / 1000) : 0;
        last = now;
        acc += dt;
        while (acc >= STEP) {
            update(STEP);
            acc -= STEP;
        }
        draw();
        requestAnimationFrame(frame);
    }

    g = fresh();
    readColors();
    resize();
    updateSoundButton();
    if (window.ResizeObserver) new ResizeObserver(resize).observe(canvas);
    else window.addEventListener('resize', resize);
    try { window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', readColors); } catch (e) {}
    requestAnimationFrame(frame);
})();
