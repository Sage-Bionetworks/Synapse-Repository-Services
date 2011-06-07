#!/bin/sh


cat src/main/resources/RepositoryWikiPrefix.txt

# only call this on localhost so that we do not accidentally put bad
# data in prod
mvn exec:java -Dexec.mainClass="org.sagebionetworks.repo.CRUDWikiGenerator" -Dorg.sagebionetworks.integrationTestUser=admin -Dexec.args="-e http://localhost:8080/repo/v1 -a $2 -u $3 -p $4" | iconv -c -f UTF-8 -t UTF-8 | grep -v "\[INFO\]" | grep -v "DEBUG \[httpclient.wire.header\] >> \"" | grep -v "WARN \[org.apache.commons.httpclient.HttpMethodBase\] Going to buffer response body of large or unknown size. Using getResponseBodyAsStream instead is recommended." | sed "s/ INFO \[org.sagebionetworks.repo.WikiGenerator\] //" | sed 's/DEBUG \[httpclient.wire.header\] << "//' | sed 's/\[\\r\]\[\\n\]"//' | uniq 

mvn exec:java -Dorg.sagebionetworks.integrationTestUser=admin -Dexec.mainClass="org.sagebionetworks.repo.ReadOnlyWikiGenerator" -Dexec.args="-e $1 -a $2 -u $3 -p $4" | iconv -c -f UTF-8 -t UTF-8 | grep -v "\[INFO\]" | grep -v "DEBUG \[httpclient.wire.header\] >> \"" | grep -v "WARN \[org.apache.commons.httpclient.HttpMethodBase\] Going to buffer response body of large or unknown size. Using getResponseBodyAsStream instead is recommended." | sed "s/ INFO \[org.sagebionetworks.repo.WikiGenerator\] //" | sed 's/DEBUG \[httpclient.wire.header\] << "//' | sed 's/\[\\r\]\[\\n\]"//' | uniq 

cat src/main/resources/RepositoryWikiSuffix.txt

