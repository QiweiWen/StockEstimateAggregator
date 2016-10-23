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
    resobj = re.search (r"(.+?),(.+?),(.+?),(.+?),(.+?),(.+?)\s*$",l)
    cusip = resobj.group(2)
    assert (len (cusip) == 8)
    analyst = resobj.group(4)
    reclvl = int(resobj.group(5))
    ancdate = resobj.group(6)
    print "insert into recommendations (\"analyst\",\"cusip\",\"ancdate\",\"reclvl\") values (\'%s\',\'%s\',\'%s\', %d);" % (analyst, cusip, ancdate, reclvl)
