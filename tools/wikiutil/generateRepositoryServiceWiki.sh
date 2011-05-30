#!/bin/sh


cat src/main/resources/RepositoryWikiPrefix.txt

mvn exec:java -Dexec.mainClass="org.sagebionetworks.repo.ReadOnlyWikiGenerator" -Dexec.args=$1 | iconv -c -f UTF-8 -t UTF-8 | grep -v "\[INFO\]" | grep -v "DEBUG \[httpclient.wire.header\] >> \"" | grep -v "WARN \[org.apache.commons.httpclient.HttpMethodBase\] Going to buffer response body of large or unknown size. Using getResponseBodyAsStream instead is recommended." | sed "s/ INFO \[org.sagebionetworks.repo.WikiGenerator\] //" | sed 's/DEBUG \[httpclient.wire.header\] << "//' | sed 's/\[\\r\]\[\\n\]"//' | uniq 

# only call this on localhost so that we do not accidentally put bad
# data in prod
mvn exec:java -Dexec.mainClass="org.sagebionetworks.repo.CRUDWikiGenerator" -Dexec.args=http://localhost:8080/repo/v1 | iconv -c -f UTF-8 -t UTF-8 | grep -v "\[INFO\]" | grep -v "DEBUG \[httpclient.wire.header\] >> \"" | grep -v "WARN \[org.apache.commons.httpclient.HttpMethodBase\] Going to buffer response body of large or unknown size. Using getResponseBodyAsStream instead is recommended." | sed "s/ INFO \[org.sagebionetworks.repo.WikiGenerator\] //" | sed 's/DEBUG \[httpclient.wire.header\] << "//' | sed 's/\[\\r\]\[\\n\]"//' | uniq 

cat src/main/resources/RepositoryWikiSuffix.txt

