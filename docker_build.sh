#!/bin/bash
# The required environment variables are:
# user - e.g. 'pjmhill'
# m2_cache_parent_folder - the folder within which .m2 is to be found
# src_folder - the folder within which the source code is found
# org_sagebionetworks_stack_iam_id - the id of the developer's AWS secret key
# org_sagebionetworks_stack_iam_key - the developer's AWS secret key
# org_sagebionetworks_stack_iam_session_token - the developer's STS token
# org_sagebionetworks_stackEncryptionKey - the stack encryption key, common to all dev builds
# org_sagebionetworks_search_enabled - when set to "true", will enable search feature and its tests
# rds_password - the password for the build database, common to all dev builds
# JOB_NAME - a unique string differentiating concurrent builds.  if omitted is the stack + user
# build_deploy - when set to any value, deploy artifacts (omit this if you do not wish to deploy)
# artifactory_username - username to deploy artifacts
# artifactory_password - password to deploy artifacts
# org_sagebionetworks_repository_database_connection_url - endpoint to mysql database for repo data
# org_sagebionetworks_table_cluster_endpoint_0 - endpoint to mysql database for user tables data
# org.sagebionetworks.doi.datacite.enabled - when set to true, enable DOI minting/editing features
# org.sagebionetworks.doi.prefix - the prefix to use when minting DOIs (e.g. 10.12345)
# org.sagebionetworks.doi.datacite.enabled - when set to true, enable DOI minting/editing features
# org.sagebionetworks.doi.datacite.username - the username used to connect to DataCite for minting DOIs
# org.sagebionetworks.doi.datacite.password - the password used to connect to DataCite for minting DOIs
# org.sagebionetworks.doi.datacite.api.endpoint - the endpoint used to connect to DataCite for minting DOIs (e.g. mds.test.datacite.org)
# org.sagebionetworks.google.cloud.enabled - when set to true, enable Google Cloud features
# org.sagebionetworks.google.cloud.key - the private key used to log into the Google Cloud service account
# org.sagebionetworks.cloudfront.private.key.id - the public key ID used for creating signed URLs for CloudFront distribution in front of the S3 data bucket
# org.sagebionetworks.cloudfront.domainname - the domain name for the CloudFront distribution in front of the S3 data bucket
# org.sagebionetworks.cloudfront.private.key.secret - the private key for the CloudFront distribution in front of the S3 data bucket

# if anything fails, stop
set -e
set -v

db_name=${stack}${user}
#used to log in to the db for setup
rds_user_name=${db_name}user


if [ ! ${JOB_NAME} ]; then
	JOB_NAME=${stack}${user}
fi

AWS_CREDS=""
if [ -n "${org_sagebionetworks_stack_iam_id}" ]  && [ -n "${org_sagebionetworks_stack_iam_key}" ]; then
	AWS_CREDS="-Dorg.sagebionetworks.stack.iam.id=${org_sagebionetworks_stack_iam_id} -Dorg.sagebionetworks.stack.iam.key=${org_sagebionetworks_stack_iam_key} "

	if [ -n "${org_sagebionetworks_stack_iam_session_token}" ]; then
		AWS_CREDS=${AWS_CREDS}"-Dorg.sagebionetworks.stack.iam.session.token=${org_sagebionetworks_stack_iam_session_token} "
	fi
fi

MVN_GOAL=install
if [ ${build_deploy} ]; then
	MVN_GOAL=deploy
	SETTINGS_XML="<settings><servers><server><id>sagebionetworks</id><username>${artifactory_username}</username><password>${artifactory_password}</password></server></servers></settings>"
fi

# the containers are ${JOB_NAME}-rds and ${JOB_NAME}-build

clean_up_container() {
if [ $(docker ps --format {{.Names}} -af name=$1) ]; then
  docker stop $1
  docker rm $1
fi
}

clean_up_volumes() {
  docker volume prune -f
}

# remove build container, if any
build_container_name=${JOB_NAME}-build
clean_up_container ${build_container_name}

clean_up_volumes

mkdir -p ${m2_cache_parent_folder}/.m2/
if [ ${SETTINGS_XML} ]; then
  echo ${SETTINGS_XML} > ${m2_cache_parent_folder}/.m2/settings.xml
fi

if [ -z ${HOME_DIR_WITHIN_CONTAINER+x} ]; then
  HOME_DIR_WITHIN_CONTAINER="/root"
fi

mysql -u${rds_user_name} -p${rds_password} -h ${org_sagebionetworks_repository_database_connection_url} -sN -e "DROP DATABASE ${db_name};CREATE DATABASE ${db_name};"
mysql -u${rds_user_name} -p${rds_password} -h ${org_sagebionetworks_table_cluster_endpoint_0} -sN -e "DROP DATABASE ${db_name};CREATE DATABASE ${db_name};"

#
# In some circumstances the hostname used to reach the RDS from within a container is different
# from the address to reach it from without a container.  E.g., on MacOS running the RDS locally,
# the RDS host is 'localhost' but within the container it's 'host.docker.internal'.
#
if [ -z ${org_sagebionetworks_repository_database_connection_url_in_container+x} ]; then
  org_sagebionetworks_repository_database_connection_url_in_container=${org_sagebionetworks_repository_database_connection_url}
fi
if [ -z ${org_sagebionetworks_table_cluster_endpoint_in_container+x} ]; then
  org_sagebionetworks_table_cluster_endpoint_in_container=${org_sagebionetworks_table_cluster_endpoint_0}
fi

# create build container and run build
docker run ${DOCKER_USER_OPTION} -i --rm --name ${build_container_name} \
-m 5500M \
-v ${m2_cache_parent_folder}/.m2:${HOME_DIR_WITHIN_CONTAINER}/.m2 \
-v ${src_folder}:/repo \
-v /etc/localtime:/etc/localtime:ro \
-e MAVEN_OPTS="-Xms256m -Xmx2048m --add-opens java.base/java.util=ALL-UNNAMED" \
-w /repo \
maven:3-amazoncorretto-21 \
bash -c "mvn clean ${MVN_GOAL} ${EXTRA_ARGS} -U \
-Dorg.sagebionetworks.repository.database.connection.url=jdbc:mysql://${org_sagebionetworks_repository_database_connection_url_in_container}/${db_name} \
-Dorg.sagebionetworks.id.generator.database.connection.url=jdbc:mysql://${org_sagebionetworks_repository_database_connection_url_in_container}/${db_name} \
-Dorg.sagebionetworks.repository.database.username=${rds_user_name} \
-Dorg.sagebionetworks.id.generator.database.username=${rds_user_name} \
${AWS_CREDS} \
-Dorg.sagebionetworks.stack.instance=${user} \
-Dorg.sagebionetworks.developer=${user} \
-Dorg.sagebionetworks.stack=${stack} \
-Dorg.sagebionetworks.table.enabled=true \
-Dorg.sagebionetworks.table.cluster.endpoint.0=${org_sagebionetworks_table_cluster_endpoint_in_container} \
-Dorg.sagebionetworks.table.cluster.schema.0=${db_name} \
-Dorg.sagebionetworks.search.enabled=${org_sagebionetworks_search_enabled} \
-Dorg.sagebionetworks.doi.datacite.enabled=${org_sagebionetworks_datacite_enabled} \
-Dorg.sagebionetworks.doi.prefix=${org_sagebionetworks_doi_prefix} \
-Dorg.sagebionetworks.doi.datacite.username=${org_sagebionetworks_datacite_username} \
-Dorg.sagebionetworks.doi.datacite.password=${org_sagebionetworks_datacite_password} \
-Dorg.sagebionetworks.doi.datacite.api.endpoint=${org_sagebionetworks_doi_datacite_api_endpoint} \
-Dorg.sagebionetworks.google.cloud.enabled=${org_sagebionetworks_google_cloud_enabled} \
-Dorg.sagebionetworks.sts.iam.arn=${org_sagebionetworks_sts_iam_arn} \
-Dorg.sagebionetworks.sts.duration.seconds=${org_sagebionetworks_sts_duration_seconds} \
-Dorg.sagebionetworks.google.cloud.key="${org_sagebionetworks_google_cloud_key}" \
-Dorg.sagebionetworks.cloudfront.private.key.id="${org_sagebionetworks_cloudfront_private_key_id}" \
-Dorg.sagebionetworks.cloudfront.private.key.secret="${org_sagebionetworks_cloudfront_private_key_secret}" \
-Dorg.sagebionetworks.cloudfront.domainname="${org_sagebionetworks_cloudfront_domainname}" \
-Dorg.sagebionetworks.docusign.enabled="${org_sagebionetworks_docusign_enabled}" \
-Dorg.sagebionetworks.docusign.private.key='${org_sagebionetworks_docusign_private_key}' \
-Dorg.sagebionetworks.docusign.account.id="${org_sagebionetworks_docusign_account_id}" \
-Dorg.sagebionetworks.docusign.user.id="${org_sagebionetworks_docusign_user_id}" \
-Dorg.sagebionetworks.docusign.integration.key="${org_sagebionetworks_docusign_integration_key}" \
-Duser.home=${HOME_DIR_WITHIN_CONTAINER}"

# above it's critical use single quotes around '${org_sagebionetworks_docusign_private_key}' since the value has space characters

clean_up_container ${build_container_name}

clean_up_volumes

