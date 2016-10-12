#!/bin/bash
make clean && make
mkdir stockprices
cd stockprices
cp ../splitter .
./splitter ../$1
rm splitter
