

create table recommendations (
    id    serial not null,
    cusip char (8) not null,
    analyst varchar (50) not null,
    reclvl smallint not null,
    ancdate date not null,
    primary key (id)
);
