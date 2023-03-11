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
    socket.onmessage = function (event) {
    	let buttonPressDiv = document.getElementById("buttonPressCount");
    	let buttonPressDivWhite = document.getElementById("buttonPressCountWhite");
    	buttonPressDiv.innerText = "BUTTON PRESSERS: " + event.data;
    	buttonPressDivWhite.innerText = "BUTTON PRESSERS: " + event.data;
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

var currentState = "released";
function sendMessage() {
    if (socket.readyState !== WebSocket.OPEN) {
        connect();
    }

    if (currentState === "released") {
        console.log("sending pressing");
        currentState = "pressing";
        socket.send("pressing");
    } else {
        console.log("sending released")
        currentState = "released";
        socket.send("released");
    }
    count++;
    if (count > 35) {
        let signup = document.getElementById("signup").style.display = 'block';
    }
}

// prevent right-click weirdness on mobile when holding the button
document.addEventListener('contextmenu', event => event.preventDefault());

window.onload = function () {
    let button = document.getElementById("pressMePls");
    console.log(button);

    let events = ["mousedown", "mouseup", "touchstart", "touchend"];

    for (const e of events) {
        button.addEventListener(
            e,
            () => {
                sendMessage();
            },
            false
        );
    }
};

