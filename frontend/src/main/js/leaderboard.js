document.addEventListener('DOMContentLoaded', function () {
    const select = document.querySelector('select[name="range"]');
    const indicator = document.getElementById('stale-indicator');

    select.addEventListener('change', function () {
        indicator.style.display = '';
    });

    document.querySelector('form').addEventListener('submit', function () {
        indicator.style.display = 'none';
    });
});
