var socket;
var count = 0;

function connect() {
    console.log("Connecting...");
    if (socket !== null && socket !== undefined) {
        socket.close(); // clean up
    }
    socket = new WebSocket(wsUrl); // from script tag in HTML

    socket.onopen = function (event) {
        console.log("Socket opened!");
        console.log(event);
    }
    // Track currently pressing names
    const currentPressers = new Set();
    socket.onmessage = function (event) {
        let msg;
        try {
            msg = JSON.parse(event.data);
        } catch {
            console.error('Bad JSON from server: ' + event.data);
            return;
        }
        switch (msg.type) {
            case 'CurrentCount': {
                let buttonPressDiv = document.getElementById("buttonPressCount");
                let buttonPressDivWhite = document.getElementById("buttonPressCountWhite");
                if (buttonPressDiv) buttonPressDiv.innerText = "BUTTON PRESSERS: " + msg.count;
                if (buttonPressDivWhite) buttonPressDivWhite.innerText = "BUTTON PRESSERS: " + msg.count;
                break;
            }
            case 'PersonPressing': {
                console.log('Person pressing:', msg.displayName);
                currentPressers.add(msg.displayName);
                window.renderFloatingPressers(Array.from(currentPressers));
                const messageDiv = document.getElementById('personPressedMessage');
                if (messageDiv) {
                    if (window.personPressedTimeout) {
                        clearTimeout(window.personPressedTimeout);
                    }
                    messageDiv.textContent = `${msg.displayName} pressed!`;
                    messageDiv.classList.add('show');
                    window.personPressedTimeout = setTimeout(() => {
                        messageDiv.classList.remove('show');
                        window.personPressedTimeout = null;
                    }, 2000);
                }
                break;
            }
            case 'PersonReleased': {
                console.log('Person released:', msg.displayName);
                currentPressers.delete(msg.displayName);
                window.renderFloatingPressers(Array.from(currentPressers));
                break;
            }
            default:
                console.error("Unknown message type received from server: ", msg.type)
                break;
        }
    }

    socket.onclose = function (event) {
        console.log("Closing, oh no!");
        console.log(event);
    }

    socket.onerror = (event) => {
        console.log("Error, oh no!");
        console.log(event);
    };
}

connect();

function sendPressState(state) {
    if (socket.readyState !== WebSocket.OPEN) {
        connect();
    }
    socket.send(JSON.stringify({ type: "PressStateChanged", state: state }));
}

function pressing() {
    sendPressState("PRESSING");
    count++;
    if (count > 15) {
        let signup = document.getElementById("signup");
        if (signup) signup.style.display = 'block';
    }
}

function released() {
    sendPressState("RELEASED");
}


// prevent right-click weirdness on mobile when holding the button
document.addEventListener('contextmenu', event => event.preventDefault());

window.onload = function () {
    let button = document.getElementById("pressMePls");
    console.log(button);

    button.addEventListener("pointerdown", () => { pressing() }, false);
    button.addEventListener("pointerup", () => { released(); }, false);
};

