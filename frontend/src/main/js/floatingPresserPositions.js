// floatingPresserPositions.js

import { Pill } from './Pill.js';
import { computeNextPillStates } from './floatingPresserPhysics.js';

/**
 * State for floating presser pills animation:
 *
 * pillState: { top: { [name]: pill }, bottom: { ... } } - DOM and animation state for each pill.
 */

let pillState = { top: {}, bottom: {} };

function getOrCreatePill(name, halfKey) {
    if (!pillState[halfKey][name]) {
        const pill = Pill.createPill(name);
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
        const W = container.clientWidth || container.offsetWidth || 600;
        const H = container.clientHeight || container.offsetHeight || 80;
        const nextStates = computeNextPillStates(pills, { W, H, frame });
        // Apply new state to DOM and objects
        for (const [index, pill] of pills.entries()) {
            const next = nextStates[index];
            pill.setCenter(next.x, next.y, W, H);
            pill.setVelocity(next.vx, next.vy);
        }
        requestAnimationFrame(step);
    }
    requestAnimationFrame(step);
}

// Tracks current pressers & which slot they're in.
// First 5 pressers go on the bottom, next 5 go on top, then they alternate.
// If a presser is removed, that just opens a slot -- doesn't move anyone else.
let nameAssignments = [];

function assignNames(names) {
    for (let i = 0; i < nameAssignments.length; i++) {
        const existingName = nameAssignments[i];
        // If an existing name isn't present, open its slot
        if (!names.includes(existingName)) {
            nameAssignments[i] = null;
        }
    }

    const newNames = names.filter((n) => !nameAssignments.includes(n));

    let curSlot = 0;
    for (const name of newNames) {
        while (curSlot < nameAssignments.length && nameAssignments[curSlot] != null) {
            curSlot++;
        }
        if (curSlot < nameAssignments.length) {
            nameAssignments[curSlot] = name;
        } else {
            nameAssignments.push(name);
        }
    }

    let bottomNames = nameAssignments.slice(0, 5);
    let topNames = nameAssignments.slice(5, 10);

    for (const [index, name] of nameAssignments.slice(10).entries()) {
        if (index % 2 == 0) {
            bottomNames.push(name);
        } else {
            topNames.push(name);
        }
    }
    topNames = topNames.filter((n) => n != null);
    bottomNames = bottomNames.filter((n) => n != null);

    return { topNames, bottomNames };
}


function renderFloatingPressers(names) {
    const topDiv = document.getElementById('floating-pressers-top');
    const botDiv = document.getElementById('floating-pressers-bottom');
    if (!topDiv || !botDiv) return;
    let { topNames, bottomNames } = assignNames(names);

    cleanupPills(topNames, 'top');
    cleanupPills(bottomNames, 'bottom');

    // Only add new pills, don't clear all
    topNames.forEach(name => {
        const pill = getOrCreatePill(name, 'top');
        if (!topDiv.contains(pill.domElement)) {
            topDiv.appendChild(pill.domElement);
        }
    });
    bottomNames.forEach(name => {
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

export { renderFloatingPressers };