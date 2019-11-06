#!/bin/sh 
#This script will change the vesion of all pom.xml files from:
#<version>develop-SNAPSHOT</version>
# to
#<version>yyyy-mm-dd-hash</version>
# The new version number is <date>-<hash>.  The <hash> is the abbreviated hash of the last commit
buildNumber=$1
if [ -z "$buildNumber" ] ;
  then buildNumber="??"
fi
echo $buildNumber
abbreviatedCommitHash="<version>"$(date +%Y-%m-%d)"-"$buildNumber"-"`git log -n 1 --pretty=format:%h`"</version>"
echo "Changing all pom.xml to version=$abbreviatedCommitHash"
#sed "s|<version>develop-SNAPSHOT</version>|$abbreviatedCommitHash|g" pom.xml > temp-pom.xml
for f in `find -name "pom.xml"` ; do
     echo "Changing version of $f"
     #We are using sed to replaces all 'develop-SNAPSHOT' verions with the new
     sed "s|<version>develop-SNAPSHOT</version>|$abbreviatedCommitHash|g" -i $f
done