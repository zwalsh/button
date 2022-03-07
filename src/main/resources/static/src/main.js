let socket = new WebSocket(wsUrl); // from script tag in HTML

socket.onopen = function (event) {
    console.log("Socket opened!");
    console.log(event);
}
socket.onmessage = function (event) {
    console.log("Got event!");
    console.log(event);

    let buttonPressDiv = document.getElementById("buttonPressCount");
    buttonPressDiv.innerText = "Button press count: " + event.data;
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

setInterval(sendMessage, 3000);

