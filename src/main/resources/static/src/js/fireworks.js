
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

    button.addEventListener("pointerdown", () => { fireworksPressing(); }, false);
    button.addEventListener("pointerup", () => { fireworksReleasing(); }, false);
}, false);


export { fireworksPressing, fireworksReleasing };
