SELECT *
FROM public.user u
WHERE EXISTS (
    SELECT
    FROM public.role r
    WHERE r.user_id = u.id AND r.role = 'USER'
) AND id NOT IN (
    (
        SELECT user1_id
        FROM public.friendship
        WHERE user2_id = :user.id
    )
    UNION
    (
        SELECT user2_id
        FROM public.friendship
        WHERE user1_id = :user.id
    )
    UNION
    (
        SELECT :user.id
    )
);
