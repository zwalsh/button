// floatingPresserPositions.js

/**
 * State for floating presser pills animation:
 *
 * pillState: { top: { [name]: pill }, bottom: { ... } } - DOM and animation state for each pill.
 */

let pillState = { top: {}, bottom: {} };

function truncateName(name) {
    return name.length > 16 ? name.slice(0, 16) + '…' : name;
}

function getOrCreatePill(name, halfKey) {
    if (!pillState[halfKey][name]) {
        const pillEl = document.createElement('div');
        pillEl.className = 'floating-presser-pill';
        pillEl.textContent = truncateName(name);
        pillEl.style.position = 'absolute';
        const pill = new window.Pill(pillEl);
        pillState[halfKey][name] = pill;
    }
    return pillState[halfKey][name];
}

function cleanupPills(namesArr, halfKey) {
    Object.keys(pillState[halfKey]).forEach(name => {
        if (!namesArr.includes(name)) {
            const pill = pillState[halfKey][name];
            pill.remove();
            delete pillState[halfKey][name];
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
        const nextStates = computeNextPillStates(pills, { W, H, frame });
        // Apply new state to DOM and objects
        for (let i = 0; i < n; i++) {
            const pill = pills[i];
            const next = nextStates[i];
            pill.setCenter(next.x, next.y, W, H);
            pill.setVelocity(next.vx, next.vy);
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
        const pill = getOrCreatePill(name, 'top');
        if (!topDiv.contains(pill.domElement)) {
            topDiv.appendChild(pill.domElement);
        }
    });
    botNames.forEach(name => {
        const pill = getOrCreatePill(name, 'bottom');
        if (!botDiv.contains(pill.domElement)) {
            botDiv.appendChild(pill.domElement);
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
