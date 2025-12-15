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
    const containerWidth = W;
    const containerHeight = H;
    function computePillTargetPosition(pillIndex, pill) {
        const angle = (2 * Math.PI * pillIndex) / pills.length;
        const ovalRadiusX = (containerWidth - 60) / 2;
        const ovalRadiusY = (containerHeight - 40) / 2;
        const targetX = containerWidth/2 + ovalRadiusX * Math.cos(angle) - pill.w/2;
        const targetY = containerHeight/2 + ovalRadiusY * Math.sin(angle) - pill.h/2;
        return { targetX, targetY };
    }
    const targets = pills.map((pill, pillIndex) => {
        return computePillTargetPosition(pillIndex, pill);
    });

    for (const [index, pill] of pills.entries()) {
        // pill has not yet been initialized; set to target location
        if (pill.x == undefined) {
            const target = targets[index];
            console.log("Pill " + JSON.stringify(pill) + " has not been initialized, setting to " + JSON.stringify(target));
            pill.x = target.targetX;
            pill.y = target.targetY;
        }
    }

    const nextPillStates = pills.map((pill, pillIndex) => {
        let fX = 0, fY = 0;
        for (let otherIndex = 0; otherIndex < pills.length; otherIndex++) {
            if (pillIndex === otherIndex) continue;
            const otherPill = pills[otherIndex];
            const dX = (pill.x + pill.w/2) - (otherPill.x + otherPill.w/2);
            const dY = (pill.y + pill.h/2) - (otherPill.y + otherPill.h/2);
            const overlapX = (pill.w + otherPill.w)/2 - Math.abs(dX);
            const overlapY = (pill.h + otherPill.h)/2 - Math.abs(dY);
            if (overlapX > 0 && overlapY > 0) {
                if (dX !== 0) fX += (dX/Math.abs(dX)) * overlapX * OVERLAP_REPEL;
                if (dY !== 0) fY += (dY/Math.abs(dY)) * overlapY * OVERLAP_REPEL;
            } else {
                const distance = Math.sqrt(dX*dX + dY*dY);
                if (distance > 0 && distance < SOFT_REPEL_DIST) {
                    fX += (dX/distance) * SOFT_REPEL_FORCE;
                    fY += (dY/distance) * SOFT_REPEL_FORCE;
                }
            }
        }
        const { targetX, targetY } = targets[pillIndex];
        fX += (targetX - pill.x) * OVAL_ATTRACT;
        fY += (targetY - pill.y) * OVAL_ATTRACT;
        if (pill.x < EDGE_MARGIN) fX += (EDGE_MARGIN - pill.x) * EDGE_REPEL;
        if (pill.y < EDGE_MARGIN) fY += (EDGE_MARGIN - pill.y) * EDGE_REPEL;
        if (pill.x + pill.w > containerWidth - EDGE_MARGIN) fX -= (pill.x + pill.w - (containerWidth - EDGE_MARGIN)) * EDGE_REPEL;
        if (pill.y + pill.h > containerHeight - EDGE_MARGIN) fY -= (pill.y + pill.h - (containerHeight - EDGE_MARGIN)) * EDGE_REPEL;
        const damping = Math.min(BASE_DAMPING + frame * DAMPING_RAMP, MAX_DAMPING);
        const velocityX = (pill.vx + fX * 0.1) * damping;
        const velocityY = (pill.vy + fY * 0.1) * damping;
        let nextX = pill.x + velocityX;
        let nextY = pill.y + velocityY;
        nextX = Math.max(0, Math.min(containerWidth - pill.w, nextX));
        nextY = Math.max(0, Math.min(containerHeight - pill.h, nextY));
        return { ...pill, x: nextX, y: nextY, vx: velocityX, vy: velocityY };
    });
    return nextPillStates;
}

if (typeof module !== 'undefined') {
    module.exports = { ringHash, assignSlots, computeNextPillStates };
} else if (typeof window !== 'undefined') {
    window.floatingPresserPhysics = { ringHash, assignSlots, computeNextPillStates };
}
