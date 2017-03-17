# customize each private build here
user=$1
org_sagebionetworks_stack_iam_id=$2
org_sagebionetworks_stack_iam_key=$3
org_sagebionetworks_stackEncryptionKey=$4

stack=dev
user_name=${stack}${user}

# the containers are ${JOB_NAME}-rds and ${JOB_NAME}-build

clean_up_container() {
if [ $(docker ps --format {{.Names}} -af name=$1) ]; then
  docker stop $1
  docker rm $1
fi
}

# remove build container, if any
build_container_name=${JOB_NAME}-build
clean_up_container ${build_container_name}
# remove rds container, if any
rds_container_name=${JOB_NAME}-rds
clean_up_container ${rds_container_name}

mkdir -p /var/lib/jenkins/${JOB_NAME}/.m2/

# could we pass these via the maven command line instead of the settings file?
echo "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\
<settings><profiles><profile><properties>\
<org.sagebionetworks.table.enabled>false</org.sagebionetworks.table.enabled>\
<org.sagebionetworks.repository.database.connection.url>\
jdbc:mysql://${rds_container_name}/${user_name}\
</org.sagebionetworks.repository.database.connection.url>\
<org.sagebionetworks.id.generator.database.connection.url>\
jdbc:mysql://${rds_container_name}/${user_name}\
</org.sagebionetworks.id.generator.database.connection.url>\
</properties></profile></profiles></settings>" > /var/lib/jenkins/${JOB_NAME}/.m2/settings.xml


# start up rds container
docker run --name ${rds_container_name} \
-e MYSQL_ROOT_PASSWORD=default-pw \
-e MYSQL_DATABASE=${user_name} \
-e MYSQL_USER=${user_name} \
-e MYSQL_PASSWORD=platform \
-v /etc/localtime:/etc/localtime:ro \
-d mysql:5.6

# create build container and run build
docker run -i --rm --name ${build_container_name} \
--link ${rds_container_name}:${rds_container_name} \
-v /var/lib/jenkins/${JOB_NAME}/.m2:/root/.m2 \
-v /var/lib/jenkins/workspace/${JOB_NAME}:/repo \
-v /etc/localtime:/etc/localtime:ro \
-w /repo \
maven:3-jdk-7 \
bash -c "mvn clean install \
-Dorg.sagebionetworks.stackEncryptionKey=${org_sagebionetworks_stackEncryptionKey} \
-Dorg.sagebionetworks.stack.iam.id=${org_sagebionetworks_stack_iam_id} \
-Dorg.sagebionetworks.stack.iam.key=${org_sagebionetworks_stack_iam_key} \
-Dorg.sagebionetworks.stack.instance=${user} \
-Dorg.sagebionetworks.developer=${user} \
-Dorg.sagebionetworks.stack=${stack} \
-Dorg.sagebionetworks.repositoryservice.endpoint=http://localhost:8080/services-repository-develop-SNAPSHOT/repo/v1 \
-Dorg.sagebionetworks.fileservice.endpoint=http://localhost:8080/services-repository-develop-SNAPSHOT/file/v1 \
-Duser.home=/root"


clean_up_container ${build_container_name}

clean_up_container ${rds_container_name}

