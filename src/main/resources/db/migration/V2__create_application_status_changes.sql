create table application_status_changes (
    id uuid primary key,
    application_id uuid not null references job_applications (id) on delete cascade,
    old_status varchar(20),
    new_status varchar(20) not null,
    changed_at timestamptz not null
);

create index idx_application_status_changes_application_id
    on application_status_changes (application_id);
