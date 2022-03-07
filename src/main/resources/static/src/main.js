let socket = new WebSocket(wsUrl); // from script tag in HTML

socket.onopen = function (event) {
    console.log("Socket opened!")
}

