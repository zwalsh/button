
$(document).ready(function(){
    $(".contact-update").click(function(){
        var button = $(this);
        $.ajax({
            type: 'POST',
            url: '/admin/contacts/update',
            data: JSON.stringify({
                contactId: button.data('contact-id'),
                active: button.data('contact-active')
            }),
            success: (data) => { console.log(data); location.reload(); },
            error: (err) => { console.log(err); alert("Error updating contact!"); },
            contentType: 'application/json',
            dataType: 'text'
        });
    });
});
