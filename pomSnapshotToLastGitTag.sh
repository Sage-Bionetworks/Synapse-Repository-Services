#!/bin/sh 
#This script will change the vesion of all pom.xml files from:
#<version>develop-SNAPSHOT</version>
# to
#<version>GitTag</version>
# The new version number is the last Git tag

newVersion="<version>"`git describe --tags`"</version>"
echo "Changing all pom.xml to version=$newVersion"
for f in `find . -name "pom.xml"` ; do
     echo "Changing version of $f"
     #We are using sed to replaces all 'develop-SNAPSHOT' verions with the new
     sed "s|<version>1.2.0</version>|<version>develop-SNAPSHOT</version>|g" -i $f
done


