#!/bin/sh

usage() {
  echo "Usage: $0 -u SynapseUsername -p SynapsePassword [-e repo endpoint] [-a auth endpoint]" >&2
  echo "endpoints default to prod if not specified" >&2
  exit 1
}

REPO_ENDPOINT="https://repo-alpha.sagebase.org/repo/v1"
AUTH_ENDPOINT="https://auth-alpha.sagebase.org/auth/v1"
USERNAME=
PASSWORD=

while getopts "a:e:p:u:" OPT; do
  case $OPT in
    a) AUTH_ENDPOINT=$OPTARG;;
    e) REPO_ENDPOINT=$OPTARG;;
    p) PASSWORD=$OPTARG;;
    u) USERNAME=$OPTARG;;
    *) usage;;
  esac
done

# Check for missing options
[ -z "$USERNAME" ] && usage && exit 1
[ -z "$PASSWORD" ] && usage && exit 1

cat src/main/resources/EntityDocumentationPrefix.html

mvn exec:java \
-Dexec.mainClass="org.sagebionetworks.client.EntityDocumentation" \
-Dexec.args="-e $REPO_ENDPOINT -a $AUTH_ENDPOINT -u $USERNAME -p $PASSWORD" | \
# ensure that all is UTF-8
iconv -c -f UTF-8 -t UTF-8 | \
# get rid of maven log output
grep -v "\[INFO\]" | \
# trim log4j prefix off of documentation 
sed 's/ INFO \[org.sagebionetworks.*\] //' | \
# trim log4j prefix off response headers
sed 's/DEBUG \[org.apache.http.headers\] << //' | \
# get rid of request headers
grep -v 'DEBUG \[org.apache.http.headers\] >>'

cat src/main/resources/EntityDocumentationSuffix.html

