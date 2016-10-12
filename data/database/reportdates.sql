create table reportdates(
    id serial not null,
    cusip char (8) not null,
    ancdate date not null,
    primary key (id)
);
