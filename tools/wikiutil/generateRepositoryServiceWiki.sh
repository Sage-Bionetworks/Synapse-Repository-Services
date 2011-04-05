#!/bin/sh


cat src/main/resources/RepositoryWikiPrefix.txt

mvn exec:java -Dexec.mainClass="org.sagebionetworks.repo.WikiGenerator" -Dexec.args=$1 | grep -v "\[INFO\]" | grep -v "DEBUG \[httpclient.wire.header\] >> \"" | grep -v "WARN \[org.apache.commons.httpclient.HttpMethodBase\] Going to buffer response body of large or unknown size. Using getResponseBodyAsStream instead is recommended." | sed "s/ INFO \[org.sagebionetworks.repo.WikiGenerator\] //" | sed 's/DEBUG \[httpclient.wire.header\] << "//' | sed 's/\[\\r\]\[\\n\]"//' | uniq 

cat src/main/resources/RepositoryWikiSuffix.txt

