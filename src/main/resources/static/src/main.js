let socket = new WebSocket(wsUrl); // from script tag in HTML

socket.onopen = function (event) {
    console.log("Socket opened!");
    console.log(event);
}
socket.onmessage = function (event) {
    console.log("Got event!");
    console.log(event);
}

socket.onclose = function (event) {
    console.log("Closing, oh no!");
    console.log(event);
}

socket.onerror = (event) => {
    console.log("Error, oh no!");
    console.log(event);
};

function sendMessage() {
   socket.send("testing");
}

setInterval(sendMessage, 3000);

