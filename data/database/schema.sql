create table monthlystock(
    cusip char (8) not null,
    year smallint not null,
    month smallint not null,
    mktval float not null,
    spret float not null
);

create table recommendations(
    analyst varchar not null,
    cusip   char(8) not null,
    ancdate date not null,
    reclvl  smallint not null
);
