#!/bin/bash

umask 027
OUTPUTFILE='/work/platform/platform.md5sums.csv'
if [ -f $OUTPUTFILE ]; then
	echo "Removing old output"
	rm $OUTPUTFILE
fi
cd source
echo "Creating new output file"
for i in `find . -type f`; do 
	echo "  $i"
	echo `md5sum $i | sed 's/\ \./\ /'` >> $OUTPUTFILE; 
done
cd ..
echo "Done"
