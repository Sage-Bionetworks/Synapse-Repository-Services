#!/bin/sh

perl -w -e 'while(! -e "./timetostop") { print "mapreduce: do not timeout please\n"; sleep 10; }' & 

perl -w -e 'for(my $i = 0; $i < 22; $i++) { sleep 1; print "I am doing real work\n"; }' 

perl -e 'bad syntax'

touch ./timetostop

exit 0