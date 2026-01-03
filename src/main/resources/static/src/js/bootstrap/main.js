import { renderFloatingPressers } from '../floatingPresserPositions.js';
import Socket from '../net/socket.js';

let count = 0;

// Track currently pressing names
const currentPressers = new Set();

const socket = new Socket({
    url: wsUrl, // injected by HomeController
    handlers: {
        onCurrentCount: (msg) => {
            let buttonPressDiv = document.getElementById("buttonPressCount");
            let buttonPressDivWhite = document.getElementById("buttonPressCountWhite");
            if (buttonPressDiv) buttonPressDiv.innerText = "BUTTON PRESSERS: " + msg.count;
            if (buttonPressDivWhite) buttonPressDivWhite.innerText = "BUTTON PRESSERS: " + msg.count;
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
        }
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

