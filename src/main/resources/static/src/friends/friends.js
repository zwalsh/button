


function successfulPost(button) {
    button.replaceWith("✅");
}


$(document).ready(function(){
    $(".send-request").click(function(){
        var button = $(this);
        $.ajax({
            type: 'POST',
            url: '/api/friendrequest',
            data: JSON.stringify({toUserId: button.data('requested-user-id')}),
            success: (data) => { successfulPost(button); },
            error: (err) => { console.log(err); alert("Error approving user!"); },
            contentType: 'application/json',
            dataType: 'json'
        });
    });
    $(".accept-request").click(function(){
        var button = $(this);
        $.ajax({
            type: 'POST',
            url: '/api/acceptrequest',
            data: JSON.stringify({fromUserId: button.data('requester-user-id')}),
            success: (data) => { successfulPost(button); },
            error: (err) => { console.log(err); alert("Error approving user!"); },
            contentType: 'application/json',
            dataType: 'json'
        });
    });
});
