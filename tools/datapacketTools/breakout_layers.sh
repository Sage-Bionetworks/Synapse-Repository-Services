#!/bin/bash
# Script to break out layers from source subdirectory

if [ $# -ne '2' ]; then
	echo "Usage: `basename $0` targetname targetdir"
	exit 65
fi

SOURCEDIR=/work/platform/source/
TARGETNAME=$1
TARGETDIR=$2

OLDDIR=`pwd`

cd $SOURCEDIR

echo
echo "Creating $TARGETNAME.zip"
zip -7rq $TARGETNAME.zip $TARGETDIR

for DIR in `find $TARGETDIR -maxdepth 1 -mindepth 1 -type d `; do
	MYSUBDIR=`echo $DIR | cut -f2 -d'/'`
	case "$MYSUBDIR" in
		expression | phenotype | genotype | mirna | cnv)	
			echo "  + Archiving and compressing $DIR into $TARGETNAME.$MYSUBDIR.zip"
			zip -7rq $TARGETNAME.$MYSUBDIR.zip $DIR $TARGETDIR/*.txt
			;;
		* ) echo "  - Skipping $DIR";;
	esac
done
echo "Completed Successfully"
cd $OLDIR
