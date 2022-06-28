INSERT INTO public.role (user_id, role) values (
    (select id from public.user where username = :username),
    :role
);
