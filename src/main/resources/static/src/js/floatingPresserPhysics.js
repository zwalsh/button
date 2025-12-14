// floatingPresserPhysics.js
// Pure functions for pill animation logic
// floatingPresserPhysics.js
// Pure functions for pill animation logic

function ringHash(name, slots) {
    let hash = 2166136261;
    for (let i = 0; i < name.length; i++) {
        hash ^= name.charCodeAt(i);
        hash += (hash << 1) + (hash << 4) + (hash << 7) + (hash << 8) + (hash << 24);
    }
    return Math.abs(hash) % slots;
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

function computeNextPillStates(pills, config) {
    const {
        W, H, frame,
        BASE_DAMPING = 0.94,
        MAX_DAMPING = 0.998,
        DAMPING_RAMP = 0.0012,
        OVERLAP_REPEL = 0.045,
        SOFT_REPEL_DIST = 110,
        SOFT_REPEL_FORCE = 0.7,
        OVAL_ATTRACT = 0.012,
        EDGE_MARGIN = 18,
        EDGE_REPEL = 0.09
    } = config;
    const n = pills.length;
    const next = pills.map((a, i) => {
        let fx = 0, fy = 0;
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
        const vx = (a.vx + fx * 0.1) * damping;
        const vy = (a.vy + fy * 0.1) * damping;
        let x = a.x + vx;
        let y = a.y + vy;
        x = Math.max(0, Math.min(W - a.w, x));
        y = Math.max(0, Math.min(H - a.h, y));
        return { ...a, x, y, vx, vy };
    });
    return next;
}

if (typeof module !== 'undefined') {
    module.exports = { ringHash, assignSlots, computeNextPillStates };
} else if (typeof window !== 'undefined') {
    window.floatingPresserPhysics = { ringHash, assignSlots, computeNextPillStates };
}
