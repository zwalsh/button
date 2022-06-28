SELECT *
FROM public.user u
WHERE NOT EXISTS (
    SELECT
    FROM public.role r
    WHERE r.user_id = u.id AND r.role = :role
);
