
// prevent right-click weirdness on mobile when holding the button
document.addEventListener('contextmenu', event => event.preventDefault());

function hideAndReveal(b) {
    let revealedElement = b.nextElementSibling;

    if (revealedElement.classList.contains("animate-count-up")) {
       countUp(revealedElement);
    }

    revealedElement.classList.remove("d-none");
    b.classList.add("d-none");
}

function countUp(counterElement) {
    let value = 0;
    const targetValue = parseInt(counterElement.innerText);

    // No point in animating if it won't tick up every frame
    if (targetValue < 10) {
       return;
    }

    const increment = targetValue / (2000 / 16); // 2000 ms at 16 frames per second

    function updateCounter() {
      value += increment;
      if (value > targetValue) {
        value = targetValue;
      }

      counterElement.innerText = Math.round(value);

      if (value < targetValue) {
        setTimeout(updateCounter, 16); // 16 ms -> 60 fps
      }
    }

    updateCounter();
}

window.addEventListener('load', function () {
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
}, false);

export { hideAndReveal, countUp };