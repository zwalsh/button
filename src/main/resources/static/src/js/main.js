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

function pressing() {
    console.log("sending pressing");
    if (socket.readyState !== WebSocket.OPEN) {
        connect();
    }
    socket.send("pressing");
    count++;
    if (count > 15) {
        let signup = document.getElementById("signup").style.display = 'block';
    }
}

function released() {
    console.log("sending released");
    if (socket.readyState !== WebSocket.OPEN) {
        connect();
    }
    socket.send("released");
}


// prevent right-click weirdness on mobile when holding the button
document.addEventListener('contextmenu', event => event.preventDefault());

window.onload = function () {
    let button = document.getElementById("pressMePls");
    console.log(button);

    button.addEventListener("pointerdown", () => { pressing(); }, false);
    button.addEventListener("pointerup", () => { released(); }, false);
};

