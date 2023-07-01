
function pressing() {
    console.log("turn on fireworks");

    Array.from(document.getElementsByClassName("fw")).forEach((elt) => {
        elt.classList.add("firework");
    });
}

function releasing() {
    console.log("turn off fireworks");


    Array.from(document.getElementsByClassName("fw")).forEach((elt) => {
        elt.classList.remove("firework");
    });
}

window.addEventListener("load", function () {
    let button = document.getElementById("pressMePls");
    console.log("setting up firework hooks");

    let pressEvents = ["mousedown", "touchstart"];

    for (const e of pressEvents) {
        button.addEventListener(
            e,
            () => {
                pressing();
            },
            false
        );
    }

    let releaseEvents = ["mouseup", "touchend"];

    for (const e of releaseEvents) {
        button.addEventListener(
            e,
            () => {
                releasing();
            },
            false
        );
    }
}, false);
