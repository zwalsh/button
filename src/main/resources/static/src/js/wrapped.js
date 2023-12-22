
// prevent right-click weirdness on mobile when holding the button
document.addEventListener('contextmenu', event => event.preventDefault());

function hideAndReveal(b) {
    console.log("Releasing " + b);
    let number = b.nextElementSibling;
    console.log(number);
    number.classList.remove("d-none");
    b.classList.add("d-none");
}


window.onload = function () {
    let buttons = document.getElementsByClassName("pressMePls");
    console.log(buttons);

    let events = ["mouseup", "touchend"];

    for (const button of buttons) {
        for (const e of events) {
            button.addEventListener(
                e,
                () => {
                    hideAndReveal(button);
                },
                false
            );
        }
    }
};

