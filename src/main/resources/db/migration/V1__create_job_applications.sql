create table job_applications (
    id uuid primary key,
    company varchar(100) not null,
    role varchar(100) not null,
    status varchar(20) not null,
    applied_date date not null,
    expected_salary_cents bigint,
    salary_currency char(3),
    notes varchar(1000),
    created_at timestamptz not null,
    updated_at timestamptz not null
);

create unique index uk_job_applications_company_role
    on job_applications (lower(company), lower(role));
