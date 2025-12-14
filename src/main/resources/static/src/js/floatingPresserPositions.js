// floatingPresserPositions.js


// Render all floating pressers into two divs above/below the button.
// Names should consistently land into one or the other bin.
// All names should be rendered as a pill.
// Pills should be packed with a force-directed algorithm to create a pleasing visual layout where they NEVER
// overlap and they arrange nicely.
function renderFloatingPressers(names) {
    const topDiv = document.getElementById('floating-pressers-top');
    const botDiv = document.getElementById('floating-pressers-bottom');
    if (!topDiv || !botDiv) return;
    topDiv.innerHTML = '';
    botDiv.innerHTML = '';
    const half = Math.ceil(names.length / 2);
    // Multi-row, adaptive grid layout
    const vSpacing = 40;
    const hSpacing = 10;
    // --- Force-directed layout ---
    function forceLayout(container, pills) {
        // Initial random positions and velocities
        const W = container.clientWidth || container.offsetWidth || 600;
        const H = container.clientHeight || container.offsetHeight || 80;
        const n = pills.length;
        const state = pills.map((pill, i) => {
            container.appendChild(pill);
            pill.style.position = 'absolute';
            // Force layout to measure size
            pill.style.left = Math.random() * (W - 100) + 'px';
            pill.style.top = Math.random() * (H - 32) + 'px';
            const w = pill.offsetWidth || 100;
            const h = pill.offsetHeight || 32;
            return {
                pill,
                x: Math.random() * (W - w),
                y: Math.random() * (H - h),
                w,
                h,
                vx: 0,
                vy: 0
            };
        });
        // Animation loop
        // --- Tunable constants ---
        const BASE_DAMPING = 0.94;
        const MAX_DAMPING = 0.998;
        const DAMPING_RAMP = 0.0012;
        const OVERLAP_REPEL = 0.045;
        const SOFT_REPEL_DIST = 110;
        const SOFT_REPEL_FORCE = 0.7;
        const CENTER_ATTRACT = 0.004;
        const EDGE_MARGIN = 18;
        const EDGE_REPEL = 0.09;
        let frame = 0;
        function step() {
            frame++;
            // Damping increases over time, up to a max
            const damping = Math.min(BASE_DAMPING + frame * DAMPING_RAMP, MAX_DAMPING);
            for (let i = 0; i < n; i++) {
                let fx = 0, fy = 0;
                const a = state[i];
                // Update width/height in case of resize
                a.w = a.pill.offsetWidth || 100;
                a.h = a.pill.offsetHeight || 32;
                for (let j = 0; j < n; j++) {
                    if (i === j) continue;
                    const b = state[j];
                    // Use bounding box collision
                    const dx = (a.x + a.w/2) - (b.x + b.w/2);
                    const dy = (a.y + a.h/2) - (b.y + b.h/2);
                    const overlapX = (a.w + b.w)/2 - Math.abs(dx);
                    const overlapY = (a.h + b.h)/2 - Math.abs(dy);
                    if (overlapX > 0 && overlapY > 0) {
                        // Repel out of overlap
                        fx += (dx/Math.abs(dx||1)) * overlapX * OVERLAP_REPEL;
                        fy += (dy/Math.abs(dy||1)) * overlapY * OVERLAP_REPEL;
                    } else {
                        // Soft repulsion at a distance
                        const dist = Math.sqrt(dx*dx + dy*dy) || 1;
                        if (dist < SOFT_REPEL_DIST) {
                            fx += (dx/dist) * SOFT_REPEL_FORCE;
                            fy += (dy/dist) * SOFT_REPEL_FORCE;
                        }
                    }
                }
                // Attract to center
                fx += (W/2 - (a.x + a.w/2)) * CENTER_ATTRACT;
                fy += (H/2 - (a.y + a.h/2)) * CENTER_ATTRACT;
                // Repel from edges
                if (a.x < EDGE_MARGIN) fx += (EDGE_MARGIN - a.x) * EDGE_REPEL;
                if (a.y < EDGE_MARGIN) fy += (EDGE_MARGIN - a.y) * EDGE_REPEL;
                if (a.x + a.w > W - EDGE_MARGIN) fx -= (a.x + a.w - (W - EDGE_MARGIN)) * EDGE_REPEL;
                if (a.y + a.h > H - EDGE_MARGIN) fy -= (a.y + a.h - (H - EDGE_MARGIN)) * EDGE_REPEL;
                // Damping (increases over time)
                a.vx = (a.vx + fx * 0.1) * damping;
                a.vy = (a.vy + fy * 0.1) * damping;
            }
            // Move and clamp
            let moving = false;
            for (let i = 0; i < n; i++) {
                const a = state[i];
                a.x += a.vx;
                a.y += a.vy;
                a.x = Math.max(0, Math.min(W - a.w, a.x));
                a.y = Math.max(0, Math.min(H - a.h, a.y));
                a.pill.style.left = a.x + 'px';
                a.pill.style.top = a.y + 'px';
                if (Math.abs(a.vx) > 0.05 || Math.abs(a.vy) > 0.05) moving = true;
            }
            if (moving) {
                requestAnimationFrame(step);
            }
        }
        step();
    }
    // Split names into top/bottom
    const topNames = names.slice(0, half);
    const botNames = names.slice(half);
    // Create pill elements
    function truncateName(name) {
        return name.length > 16 ? name.slice(0, 16) + '…' : name;
    }
    const topPills = topNames.map(name => {
        const pill = document.createElement('div');
        pill.className = 'floating-presser-pill';
        pill.textContent = truncateName(name);
        return pill;
    });
    const botPills = botNames.map(name => {
        const pill = document.createElement('div');
        pill.className = 'floating-presser-pill';
        pill.textContent = truncateName(name);
        return pill;
    });
    forceLayout(topDiv, topPills);
    forceLayout(botDiv, botPills);
}

// Export for use in main.js or elsewhere
if (typeof window !== 'undefined') {
    window.renderFloatingPressers = renderFloatingPressers;
}
if (typeof module !== 'undefined') {
    module.exports = { renderFloatingPressers };
}
