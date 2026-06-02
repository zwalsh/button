
$(document).ready(function() {
    var alert = $('#savedAlert');
    if (alert.length) {
        setTimeout(function() {
            alert.alert('close');
        }, 3000);
    }
});
