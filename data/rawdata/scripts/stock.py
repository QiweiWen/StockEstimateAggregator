import sys
import re
argnum = len (sys.argv)
if (argnum != 2):
    print "look, I came here for an argument"
    sys.exit()

infile = sys.argv[1]
lines = [line.rstrip('\n') for line in open(infile)]
linenum = 0;
for l in lines:
    linenum = linenum + 1
    if (linenum == 1):
        continue;
    resobj = re.search (r"(.+?),(.+?),(.+?),(.+?),(.+?),(.+?),(.+?),(.+?)\s*$",l)
    date = resobj.group (2)
    cusip = resobj. group(3)
    assert (len (cusip) == 8)
    price = abs(float(resobj.group (4)))
    outstanding = int(resobj.group (7))
    mktval = price * outstanding
    bid = float (resobj.group (5))
    ask = float (resobj.group (6))
    spret = float(resobj.group (8))
    dateobj = re.search (r"^([0-9]{4})([0-9]{2})", date)
    year = int(dateobj.group (1))
    month = int(dateobj.group (2))
    print "insert into monthlystock (\"cusip\", \"year\", \"month\",\"mktval\",\"spret\",\"closbid\",\"closask\") values (\'%s\', %d, %d, %lf, %lf,%lf,%lf);" % (cusip, year, month, mktval, spret,bid,ask)
 
