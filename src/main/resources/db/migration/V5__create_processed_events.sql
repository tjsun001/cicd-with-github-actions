create table if not exists processed_events (
                                                event_id uuid primary key,
                                                processed_at timestamptz not null default now(),
                                                status varchar(32) not null,
                                                error text null
);
