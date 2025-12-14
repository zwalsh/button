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

let pillState = { top: {}, bottom: {} };

function truncateName(name) {
    return name.length > 16 ? name.slice(0, 16) + '…' : name;
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
        pill.style.position = 'absolute';
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
    let frame = 0;
    function step() {
        frame++;
        const pills = Object.values(pillState[halfKey]);
        const n = pills.length;
        const W = container.clientWidth || container.offsetWidth || 600;
        const H = container.clientHeight || container.offsetHeight || 80;
        // Update width/height for each pill
        for (let i = 0; i < n; i++) {
            const a = pills[i];
            a.w = a.pill.offsetWidth || 100;
            a.h = a.pill.offsetHeight || 32;
        }
        // Prepare pure state
        const pillStates = pills.map(a => ({
            ...a,
            // Defensive: ensure w/h present
            w: a.w || 100,
            h: a.h || 32
        }));
        const nextStates = computeNextPillStates(pillStates, { W, H, frame });
        // Apply new state to DOM and objects
        for (let i = 0; i < n; i++) {
            const a = pills[i];
            const next = nextStates[i];
            a.x = next.x;
            a.y = next.y;
            a.vx = next.vx;
            a.vy = next.vy;
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
        if (!topDiv.contains(state.pill)) {
            topDiv.appendChild(state.pill);
        }
    });
    botNames.forEach(name => {
        const state = getOrCreatePill(name, 'bottom');
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
}

// Export for use in main.js or elsewhere
if (typeof window !== 'undefined') {
    window.renderFloatingPressers = renderFloatingPressers;
}

if (typeof module !== 'undefined') {
    module.exports = { renderFloatingPressers };
}
