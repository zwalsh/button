
function fireworksPressing() {
    console.log("turn on fireworks");

    Array.from(document.getElementsByClassName("fw")).forEach((elt) => {
        elt.classList.add("firework");
    });
}

function fireworksReleasing() {
    console.log("turn off fireworks");

    Array.from(document.getElementsByClassName("fw")).forEach((elt) => {
        elt.classList.remove("firework");
    });
}

window.addEventListener("load", function () {
    let button = document.getElementById("pressMePls");
    console.log("setting up firework hooks");

    let pressEvents = ["pointerdown"];

    for (const e of pressEvents) {
        button.addEventListener(
            e,
            () => {
                fireworksPressing();
            },
            false
        );
    }

    let releaseEvents = ["pointerup"];

    for (const e of releaseEvents) {
        button.addEventListener(
            e,
            () => {
                fireworksReleasing();
            },
            false
        );
    }
}, false);
