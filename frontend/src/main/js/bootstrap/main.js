import { renderFloatingPressers } from '../floatingPresserPositions.js';
import Socket from '../net/socket.js';

let count = 0;

// Track currently pressing names
const currentPressers = new Set();

function setPresserCount(n) {
    let buttonPressDiv = document.getElementById("buttonPressCount");
    let buttonPressDivWhite = document.getElementById("buttonPressCountWhite");
    if (buttonPressDiv) buttonPressDiv.innerText = "BUTTON PRESSERS: " + n;
    if (buttonPressDivWhite) buttonPressDivWhite.innerText = "BUTTON PRESSERS: " + n;
}

function setDailyStats({ uniquePressers, peakConcurrent, totalPresses }) {
    const presserWord = uniquePressers === 1 ? 'presser' : 'pressers';
    const pressWord = totalPresses === 1 ? 'press' : 'presses';
    const text = `${uniquePressers} ${presserWord} today · peak ${peakConcurrent} · ${totalPresses} ${pressWord}`;
    const el = document.getElementById('dailyStats');
    const elWhite = document.getElementById('dailyStatsWhite');
    if (el) el.innerText = text;
    if (elWhite) elWhite.innerText = text;
}

const socket = new Socket({
    url: wsUrl, // injected by HomeController
    handlers: {
        onCurrentCount: (msg) => {
            setPresserCount(msg.count);
        },
        onPersonPressing: (msg) => {
            currentPressers.add(msg.displayName);
            renderFloatingPressers(Array.from(currentPressers));
        },
        onPersonReleased: (msg) => {
            currentPressers.delete(msg.displayName);
            setTimeout(() => {
                renderFloatingPressers(Array.from(currentPressers));
            }, 100);
        },
        onSnapshot: (msg) => {
            currentPressers.clear();
            msg.names.forEach(name => currentPressers.add(name));
            renderFloatingPressers(Array.from(currentPressers));
            setPresserCount(msg.count);
            if (msg.dailyStats) setDailyStats(msg.dailyStats);
        },
        onDailyStats: (msg) => setDailyStats(msg)
    }
});

function pressing() {
    socket.sendPressing();
    count++;
    if (count > 15) {
        let signup = document.getElementById("signup");
        if (signup) signup.style.display = 'block';
    }
}

function released() {
    socket.sendReleased();
}

// prevent right-click weirdness on mobile when holding the button
document.addEventListener('contextmenu', event => event.preventDefault());

window.addEventListener('load', function () {
    let button = document.getElementById("pressMePls");

    button.addEventListener("pointerdown", () => { pressing(); }, false);
    button.addEventListener("pointerup", () => { released(); }, false);
}, false);

