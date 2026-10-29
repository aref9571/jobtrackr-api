alter table job_applications
    add column owner_id uuid not null references users (id) on delete cascade;

drop index uk_job_applications_company_role;

create unique index uk_job_applications_owner_company_role
    on job_applications (owner_id, lower(company), lower(role));
