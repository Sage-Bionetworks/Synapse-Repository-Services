#!/bin/bash
# The required environment variables are:
# user - e.g. 'pjmhill'
# m2_cache_parent_folder - the folder within which .m2 is to be found
# src_folder - the folder within which the source code is found
# org_sagebionetworks_stack_iam_id - the id of the developer's AWS secret key
# org_sagebionetworks_stack_iam_key - the developer's AWS secret key
# org_sagebionetworks_stackEncryptionKey - the stack encryption key, common to all dev builds
# org_sagebionetworks_search_enabled - when set to "true", will enable search feature and its tests
# rds_password - the password for the build database, common to all dev builds
# JOB_NAME - a unique string differentiating concurrent builds.  if omitted is the stack + user
# build_deploy - when set to "true" deploy artifacts
# artifactory_username - username to deploy artifacts
# artifactory_password - password to deploy artifacts

# if anything fails, stop
set -e

rds_user_name=${stack}${user}

if [ ! ${JOB_NAME} ]; then
	JOB_NAME=${stack}${user}
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

clean_up_network() {
if [ $(docker network ls | grep -q $1 && echo $?) ]; then
  docker network rm $1
fi
}

clean_up_volumes() {
  docker volume prune -f
}

# remove build container, if any
build_container_name=${JOB_NAME}-build
clean_up_container ${build_container_name}
# remove rds container, if any
rds_container_name=${JOB_NAME}-rds
clean_up_container ${rds_container_name}
# remove the network if it's still there from last time
network_name=${JOB_NAME}
clean_up_network ${network_name}

clean_up_volumes

mkdir -p ${m2_cache_parent_folder}/.m2/
if [ ${SETTINGS_XML} ]; then
  echo ${SETTINGS_XML} > ${m2_cache_parent_folder}/.m2/settings.xml
fi

docker network create --driver bridge ${network_name}

# start up rds container
docker run --name ${rds_container_name} \
--network=${network_name} \
-m 1500M \
-e MYSQL_ROOT_PASSWORD=default-pw \
-e MYSQL_DATABASE=${rds_user_name} \
-e MYSQL_USER=${rds_user_name} \
-e MYSQL_PASSWORD=${rds_password} \
-v /etc/localtime:/etc/localtime:ro \
-d mysql:5.6

# make sure RDS is ready to go
sleep 20

tables_schema_name=${rds_user_name}tables
docker exec ${rds_container_name} mysql -uroot -pdefault-pw -sN -e "CREATE SCHEMA ${tables_schema_name};"
docker exec ${rds_container_name} mysql -uroot -pdefault-pw -sN -e "GRANT ALL ON ${tables_schema_name}.* TO '${rds_user_name}'@'%';"

# create build container and run build
docker run -i --rm --name ${build_container_name} \
-m 5500M \
--network=${network_name} \
--link ${rds_container_name}:${rds_container_name} \
-v ${m2_cache_parent_folder}/.m2:/root/.m2 \
-v ${src_folder}:/repo \
-v /etc/localtime:/etc/localtime:ro \
-e MAVEN_OPTS="-Xms256m -Xmx2048m -XX:MaxPermSize=512m" \
-w /repo \
maven:3-jdk-8 \
bash -c "mvn clean ${MVN_GOAL} \
-Dorg.sagebionetworks.repository.database.connection.url=jdbc:mysql://${rds_container_name}/${rds_user_name} \
-Dorg.sagebionetworks.id.generator.database.connection.url=jdbc:mysql://${rds_container_name}/${rds_user_name} \
-Dorg.sagebionetworks.stackEncryptionKey=${org_sagebionetworks_stackEncryptionKey} \
-Dorg.sagebionetworks.stack.iam.id=${org_sagebionetworks_stack_iam_id} \
-Dorg.sagebionetworks.stack.iam.key=${org_sagebionetworks_stack_iam_key} \
-Dorg.sagebionetworks.stack.instance=${user} \
-Dorg.sagebionetworks.developer=${user} \
-Dorg.sagebionetworks.stack=${stack} \
-Dorg.sagebionetworks.table.enabled=true \
-Dorg.sagebionetworks.table.cluster.endpoint.0=${rds_container_name} \
-Dorg.sagebionetworks.table.cluster.schema.0=${tables_schema_name} \
-Dorg.sagebionetworks.search.enabled=${org_sagebionetworks_search_enabled} \
-Duser.home=/root"

clean_up_container ${build_container_name}

clean_up_container ${rds_container_name}

clean_up_network ${network_name}

clean_up_volumes

