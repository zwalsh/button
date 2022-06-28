select requester.id, requester.username, requester.hash,
requested.id, requested.username, requested.hash
from public.friend_request fr
join public.user requester on fr.requester_id = requester.id
join public.user requested on fr.requested_id = requested.id
where fr.requester_id = :user.id;
