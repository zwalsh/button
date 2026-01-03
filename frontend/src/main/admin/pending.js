


function successfulPost(button) {
    button.replaceWith("✅");
}


$(document).ready(function(){
    $(".user-approve").click(function(){
        var button = $(this);
        $.ajax({
            type: 'POST',
            url: '/admin/pending/approve',
            data: JSON.stringify({userId: button.data('user-id')}),
            success: (data) => { successfulPost(button); },
            error: (err) => { console.log(err); alert("Error approving user!"); },
            contentType: 'application/json',
            dataType: 'json'
        });
    });
});
