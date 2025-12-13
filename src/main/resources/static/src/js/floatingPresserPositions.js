// floatingPresserPositions.js
// Utility to deterministically compute (x, y) positions for floating pressers
// per docs/pressers-floating-plan.md
// Last updated: 2025-12-13T21:47:08.797Z
console.log('[floatingPresserPositions.js] loaded');

// Hash a string to a 32-bit integer
function hashString(str) {
    let hash = 5381;
    for (let i = 0; i < str.length; i++) {
        hash = ((hash << 5) + hash) + str.charCodeAt(i);
        hash = hash & 0xffffffff;
    }
    return Math.abs(hash);
}

// Generate a pseudo-random number [0, 1) from a seed
function seededRandom(seed) {
    // Mulberry32 PRNG
    let t = seed + 0x6D2B79F5;
    t = Math.imul(t ^ t >>> 15, t | 1);
    t ^= t + Math.imul(t ^ t >>> 7, t | 61);
    return ((t ^ t >>> 14) >>> 0) / 4294967296;
}

// Compute a (x, y) position in percent (0-100), avoiding the button "no-fly zone"
function computeFloatingPresserPosition(name, salt = 0) {
    const seed = hashString(name + ':' + salt);
    // No-fly zone: x 30-70%, y 40-60%
    const noFly = { xMin: 30, xMax: 70, yMin: 40, yMax: 60 };
    const edgePad = 6; // percent padding from edges
    let tries = 0;
    while (tries < 10) {
        // Generate x, y in [edgePad, 100-edgePad]
        const x = edgePad + seededRandom(seed + tries) * (100 - 2 * edgePad);
        const y = edgePad + seededRandom(seed + tries + 1000) * (100 - 2 * edgePad);
        // Check if in no-fly zone
        if (!(x >= noFly.xMin && x <= noFly.xMax && y >= noFly.yMin && y <= noFly.yMax)) {
            return { x, y };
        }
        tries++;
    }
    // Fallback: just return last computed
    return { x: edgePad + seededRandom(seed) * (100 - 2 * edgePad),
             y: edgePad + seededRandom(seed + 1000) * (100 - 2 * edgePad) };
}

// Exported: get all positions for a list of names
function getFloatingPresserPositions(names) {
    // Returns: { name, x, y }[]
    return names.map((name, i) => {
        // Optionally, use i as salt for minimal overlap avoidance
        return { name, ...computeFloatingPresserPosition(name, i) };
    });
}

// --- Force-directed organic packing for floating pressers ---
// Button exclusion zone (percent of viewport)
const BUTTON_ZONE = { xMin: 30, xMax: 70, yMin: 40, yMax: 60 };
const EDGE_PAD = 6; // percent
const PILL_RADIUS = 7; // percent, for collision
const ITERATIONS = 60;

// Assign half to top, half to bottom
function splitNames(names) {
    const mid = Math.ceil(names.length / 2);
    return [names.slice(0, mid), names.slice(mid)];
}

// Deterministic seeded random for initial placement
function seededPlacement(name, region) {
    const seed = hashString(name + ':' + region);
    const x = EDGE_PAD + seededRandom(seed) * (100 - 2 * EDGE_PAD);
    let y;
    if (region === 'top') {
        y = EDGE_PAD + seededRandom(seed + 1) * (BUTTON_ZONE.yMin - 2 * EDGE_PAD);
    } else {
        y = BUTTON_ZONE.yMax + seededRandom(seed + 2) * (100 - BUTTON_ZONE.yMax - EDGE_PAD);
    }
    return { x, y };
}

// Force-directed packing
function packPositions(names, region) {
    // Start with deterministic positions
    let nodes = names.map(name => ({
        name,
        ...seededPlacement(name, region),
        vx: 0,
        vy: 0
    }));
    for (let iter = 0; iter < ITERATIONS; iter++) {
        // Repel overlapping nodes
        for (let i = 0; i < nodes.length; i++) {
            for (let j = i + 1; j < nodes.length; j++) {
                const a = nodes[i], b = nodes[j];
                const dx = a.x - b.x, dy = a.y - b.y;
                const dist = Math.sqrt(dx * dx + dy * dy);
                if (dist < PILL_RADIUS * 2) {
                    const overlap = PILL_RADIUS * 2 - dist;
                    const nx = dx / (dist || 1), ny = dy / (dist || 1);
                    a.vx += nx * overlap * 0.05;
                    a.vy += ny * overlap * 0.05;
                    b.vx -= nx * overlap * 0.05;
                    b.vy -= ny * overlap * 0.05;
                }
            }
        }
        // Keep inside allowed region
        for (const n of nodes) {
            if (n.x < EDGE_PAD) n.vx += (EDGE_PAD - n.x) * 0.1;
            if (n.x > 100 - EDGE_PAD) n.vx -= (n.x - (100 - EDGE_PAD)) * 0.1;
            if (region === 'top') {
                if (n.y < EDGE_PAD) n.vy += (EDGE_PAD - n.y) * 0.1;
                if (n.y > BUTTON_ZONE.yMin - EDGE_PAD) n.vy -= (n.y - (BUTTON_ZONE.yMin - EDGE_PAD)) * 0.1;
            } else {
                if (n.y < BUTTON_ZONE.yMax + EDGE_PAD) n.vy += (BUTTON_ZONE.yMax + EDGE_PAD - n.y) * 0.1;
                if (n.y > 100 - EDGE_PAD) n.vy -= (n.y - (100 - EDGE_PAD)) * 0.1;
            }
        }
        // Apply velocity
        for (const n of nodes) {
            n.x += n.vx;
            n.y += n.vy;
            n.vx *= 0.5;
            n.vy *= 0.5;
        }
    }
    return nodes;
}

// Render all floating pressers into two divs above/below the button
function renderFloatingPressers(names) {
    const topDiv = document.getElementById('floating-pressers-top');
    const botDiv = document.getElementById('floating-pressers-bottom');
    if (!topDiv || !botDiv) return;
    topDiv.innerHTML = '';
    botDiv.innerHTML = '';
    const [topNames, botNames] = splitNames(names);
    // Wait for layout to ensure bounding boxes are correct
    requestAnimationFrame(() => {
        const topRect = topDiv.getBoundingClientRect();
        const botRect = botDiv.getBoundingClientRect();
        const topNodes = packPositions(topNames, 'top');
        const botNodes = packPositions(botNames, 'bottom');
        for (const n of topNodes) {
            const pill = document.createElement('span');
            pill.className = 'floating-presser-pill';
            pill.textContent = n.name;
            pill.style.position = 'absolute';
            pill.style.left = (n.x / 100 * topRect.width) + 'px';
            pill.style.top = (n.y / 100 * topRect.height) + 'px';
            pill.style.transform = 'translate(-50%, -50%)';
            pill.title = n.name;
            topDiv.appendChild(pill);
        }
        for (const n of botNodes) {
            const pill = document.createElement('span');
            pill.className = 'floating-presser-pill';
            pill.textContent = n.name;
            pill.style.position = 'absolute';
            pill.style.left = (n.x / 100 * botRect.width) + 'px';
            pill.style.top = (n.y / 100 * botRect.height) + 'px';
            pill.style.transform = 'translate(-50%, -50%)';
            pill.title = n.name;
            botDiv.appendChild(pill);
        }
    });
}

// Export for use in main.js or elsewhere
if (typeof window !== 'undefined') {
    window.getFloatingPresserPositions = getFloatingPresserPositions;
    window.computeFloatingPresserPosition = computeFloatingPresserPosition;
    window.renderFloatingPressers = renderFloatingPressers;
    console.log('[floatingPresserPositions.js] window.getFloatingPresserPositions assigned:', typeof window.getFloatingPresserPositions);
}
if (typeof module !== 'undefined') {
    module.exports = { getFloatingPresserPositions, computeFloatingPresserPosition, renderFloatingPressers };
}
