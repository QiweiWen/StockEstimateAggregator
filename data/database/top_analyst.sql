create table top_analysts as
select ofo.analyst, ofo. cusip, ofo. ancdate, ofo.reclvl from
((select analyst as an, count(*) as co from recommendations group by analyst) as foo
join
recommendations bar on foo.an = bar.analyst) as ofo where ofo.co > 50 order by ofo.analyst desc;
