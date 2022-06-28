SELECT * FROM public.user
WHERE id IN (
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
);
