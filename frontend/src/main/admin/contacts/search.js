document.addEventListener('DOMContentLoaded', function () {
    var timer;
    var input = document.querySelector('input[name="query"]');
    if (input) {
        input.addEventListener('input', function () {
            clearTimeout(timer);
            timer = setTimeout(function () {
                document.querySelector('form').submit();
            }, 250);
        });
    }
});
