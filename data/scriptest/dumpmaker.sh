boilerplate="insert into reportdates (\"cusip\", \"ancdate\") "
boilerplate2="values ('"
regex="^([0-9]+),\s*([0-9]+)"

while read line;do
    if [[ $line =~ $regex ]]; then
        string=$boilerplate$boilerplate2;
        string=$string${BASH_REMATCH[1]};
        string=$string"'";
        string=$string",'";
        string=$string${BASH_REMATCH[2]};
        string=$string"');";
        echo $string;
        
    fi;
done < $1 >$2
