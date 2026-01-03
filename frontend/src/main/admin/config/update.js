
$(document).ready(function(){
    $(".cube-update").click(function(){
        var button = $(this);
        $.ajax({
            type: 'POST',
            url: '/admin/config/update-cube',
            data: JSON.stringify({
                isCube: button.data('cube')
            }),
            success: (data) => { console.log(data); location.reload(); },
            error: (err) => { console.log(err); alert("Error updating cube!"); },
            contentType: 'application/json',
            dataType: 'text'
        });
    });
});
