// floatingPresserPositions.js

/**
 * State for floating presser pills animation:
 *
 * 1. nameToPosition: { [name]: { x, y } } - persists pill positions across renders for each name.
 * 2. pillState: { top: { [name]: { pill, x, y, vx, vy } }, bottom: { ... } } - DOM and animation state for each pill.
 * 3. activeNames: string[] - current set of names to display (implicit, passed to render).
 *
 * Usage:
 * - On each render, reuse nameToPosition for existing names, assign new random positions for new names.
 * - Remove positions for names no longer present.
 * - This keeps persistent names stable and only animates new/removed names.
 */

// Persistent mapping of name to position
const nameToPosition = {};

// Consistent ring hash for stable slot assignment
function ringHash(name, slots) {
    let hash = 2166136261;
    for (let i = 0; i < name.length; i++) {
        hash ^= name.charCodeAt(i);
        hash += (hash << 1) + (hash << 4) + (hash << 7) + (hash << 8) + (hash << 24);
    }
    return Math.abs(hash) % slots;
}

let pillState = { top: {}, bottom: {} };

function truncateName(name) {
    return name.length > 16 ? name.slice(0, 16) + '…' : name;
}

function assignSlots(namesArr) {
    const slotMap = {};
    namesArr.forEach(name => {
        slotMap[name] = ringHash(name, namesArr.length);
    });
    const used = new Set();
    namesArr.forEach(name => {
        let slot = slotMap[name];
        while (used.has(slot)) slot = (slot + 1) % namesArr.length;
        slotMap[name] = slot;
        used.add(slot);
    });
    return slotMap;
}

function getOrCreatePill(name, halfKey) {
    if (!pillState[halfKey][name]) {
        const pill = document.createElement('div');
        pill.className = 'floating-presser-pill';
        pill.textContent = truncateName(name);
        // Use persistent position if available, else random
        let pos = nameToPosition[name];
        if (!pos) {
            pos = {
                x: Math.random() * 500 + 50, // fallback, will be set in forceLayout
                y: Math.random() * 40 + 10
            };
            nameToPosition[name] = pos;
        }
        pill.style.left = pos.x + 'px';
        pill.style.top = pos.y + 'px';
        pillState[halfKey][name] = { pill, x: pos.x, y: pos.y, vx: 0, vy: 0 };
    }
    return pillState[halfKey][name];
}

function cleanupPills(namesArr, halfKey) {
    Object.keys(pillState[halfKey]).forEach(name => {
        if (!namesArr.includes(name)) {
            const pill = pillState[halfKey][name].pill;
            if (pill.parentNode) pill.parentNode.removeChild(pill);
            delete pillState[halfKey][name];
            // Remove from persistent position map
            delete nameToPosition[name];
        }
    });
}

// --- Animation loop for each halfKey ---
function animatePills(halfKey, container) {
    const BASE_DAMPING = 0.94;
    const MAX_DAMPING = 0.998;
    const DAMPING_RAMP = 0.0012;
    const OVERLAP_REPEL = 0.045;
    const SOFT_REPEL_DIST = 110;
    const SOFT_REPEL_FORCE = 0.7;
    const OVAL_ATTRACT = 0.012;
    const EDGE_MARGIN = 18;
    const EDGE_REPEL = 0.09;
    let frame = 0;
    function step() {
        frame++;
        const pills = Object.values(pillState[halfKey]);
        const n = pills.length;
        const W = container.clientWidth || container.offsetWidth || 600;
        const H = container.clientHeight || container.offsetHeight || 80;
        for (let i = 0; i < n; i++) {
            let fx = 0, fy = 0;
            const a = pills[i];
            a.w = a.pill.offsetWidth || 100;
            a.h = a.pill.offsetHeight || 32;
            for (let j = 0; j < n; j++) {
                if (i === j) continue;
                const b = pills[j];
                const dx = (a.x + a.w/2) - (b.x + b.w/2);
                const dy = (a.y + a.h/2) - (b.y + b.h/2);
                const overlapX = (a.w + b.w)/2 - Math.abs(dx);
                const overlapY = (a.h + b.h)/2 - Math.abs(dy);
                if (overlapX > 0 && overlapY > 0) {
                    if (dx !== 0) fx += (dx/Math.abs(dx)) * overlapX * OVERLAP_REPEL;
                    if (dy !== 0) fy += (dy/Math.abs(dy)) * overlapY * OVERLAP_REPEL;
                } else {
                    const dist = Math.sqrt(dx*dx + dy*dy);
                    if (dist > 0 && dist < SOFT_REPEL_DIST) {
                        fx += (dx/dist) * SOFT_REPEL_FORCE;
                        fy += (dy/dist) * SOFT_REPEL_FORCE;
                    }
                }
            }
            const angle = (2 * Math.PI * i) / n;
            const ovalA = (W - 60) / 2;
            const ovalB = (H - 40) / 2;
            const targetX = W/2 + ovalA * Math.cos(angle) - a.w/2;
            const targetY = H/2 + ovalB * Math.sin(angle) - a.h/2;
            fx += (targetX - a.x) * OVAL_ATTRACT;
            fy += (targetY - a.y) * OVAL_ATTRACT;
            if (a.x < EDGE_MARGIN) fx += (EDGE_MARGIN - a.x) * EDGE_REPEL;
            if (a.y < EDGE_MARGIN) fy += (EDGE_MARGIN - a.y) * EDGE_REPEL;
            if (a.x + a.w > W - EDGE_MARGIN) fx -= (a.x + a.w - (W - EDGE_MARGIN)) * EDGE_REPEL;
            if (a.y + a.h > H - EDGE_MARGIN) fy -= (a.y + a.h - (H - EDGE_MARGIN)) * EDGE_REPEL;
            const damping = Math.min(BASE_DAMPING + frame * DAMPING_RAMP, MAX_DAMPING);
            a.vx = (a.vx + fx * 0.1) * damping;
            a.vy = (a.vy + fy * 0.1) * damping;
        }
        for (let i = 0; i < n; i++) {
            const a = pills[i];
            a.x += a.vx;
            a.y += a.vy;
            a.x = Math.max(0, Math.min(W - a.w, a.x));
            a.y = Math.max(0, Math.min(H - a.h, a.y));
            a.pill.style.left = a.x + 'px';
            a.pill.style.top = a.y + 'px';
            if (a.name) {
                nameToPosition[a.name] = { x: a.x, y: a.y };
            }
        }
        requestAnimationFrame(step);
    }
    requestAnimationFrame(step);
}

function renderFloatingPressers(names) {
    const topDiv = document.getElementById('floating-pressers-top');
    const botDiv = document.getElementById('floating-pressers-bottom');
    if (!topDiv || !botDiv) return;
    let topNames = [];
    let botNames = [];
    names.forEach(name => {
        (ringHash(name, 2) === 0 ? topNames : botNames).push(name);
    });

    const topSlots = assignSlots(topNames);
    const botSlots = assignSlots(botNames);

    cleanupPills(topNames, 'top');
    cleanupPills(botNames, 'bottom');

    // Only add new pills, don't clear all
    topNames.forEach(name => {
        const state = getOrCreatePill(name, 'top');
        state.pill.style.position = 'absolute';
        if (!topDiv.contains(state.pill)) {
            topDiv.appendChild(state.pill);
        }
    });
    botNames.forEach(name => {
        const state = getOrCreatePill(name, 'bottom');
        state.pill.style.position = 'absolute';
        if (!botDiv.contains(state.pill)) {
            botDiv.appendChild(state.pill);
        }
    });
}

// Start the animation loops once at load
if (typeof window !== 'undefined') {
    window.addEventListener('DOMContentLoaded', () => {
        const topDiv = document.getElementById('floating-pressers-top');
        const botDiv = document.getElementById('floating-pressers-bottom');
        if (topDiv) animatePills('top', topDiv);
        if (botDiv) animatePills('bottom', botDiv);
    });
    window.renderFloatingPressers = renderFloatingPressers;
}

// Export for use in main.js or elsewhere
if (typeof window !== 'undefined') {
    window.renderFloatingPressers = renderFloatingPressers;
}

if (typeof module !== 'undefined') {
    module.exports = { renderFloatingPressers };
}
