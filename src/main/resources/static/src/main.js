let socket = new WebSocket(wsUrl); // from script tag in HTML

socket.onopen = function (event) {
    console.log("Socket opened!");
    console.log(event);
}
socket.onmessage = function (event) {
    let buttonPressDiv = document.getElementById("buttonPressCount");
    buttonPressDiv.innerText = "BUTTON PRESSERS: " + event.data;
}

socket.onclose = function (event) {
    console.log("Closing, oh no!");
    console.log(event);
}

socket.onerror = (event) => {
    console.log("Error, oh no!");
    console.log(event);
};

var currentState = "released";
function sendMessage() {
    if (currentState === "released") {
        socket.send("pressing");
        currentState = "pressing";
    } else {
        socket.send("released");
        currentState = "released";
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

