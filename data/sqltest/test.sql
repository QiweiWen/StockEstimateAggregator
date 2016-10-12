select cusip, ancdate 
 from ((select max(ancdate) as m from reportdates where cusip = '03783310' and ancdate < '2009-08-08') as foo
 join
 reportdates bar on bar.ancdate = foo.m) as foobar
 where foobar.cusip = '03783310'
