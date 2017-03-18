
stack=dev
rds_user_name=${stack}${user}

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

# ultimately this line can be removed
#rm /var/lib/jenkins/${JOB_NAME}/.m2/settings.xml

echo "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\
<settings><profiles><profile><properties>\
<org.sagebionetworks.repository.database.connection.url>jdbc:mysql://${rds_container_name}/${rds_user_name}</org.sagebionetworks.repository.database.connection.url>\
<org.sagebionetworks.id.generator.database.connection.url>jdbc:mysql://${rds_container_name}/${rds_user_name}</org.sagebionetworks.id.generator.database.connection.url>\
<org.sagebionetworks.stackEncryptionKey>${org_sagebionetworks_stackEncryptionKey}</org.sagebionetworks.stackEncryptionKey>\
<org.sagebionetworks.stack.iam.id>${org_sagebionetworks_stack_iam_id}</org.sagebionetworks.stack.iam.id>\
<org.sagebionetworks.stack.iam.key>${org_sagebionetworks_stack_iam_key}</org.sagebionetworks.stack.iam.key>\
<org.sagebionetworks.stack.instance>${user}</org.sagebionetworks.stack.instance>\
<org.sagebionetworks.developer>${user}</org.sagebionetworks.developer>\
<org.sagebionetworks.stack>${stack}</org.sagebionetworks.stack>\
<org.sagebionetworks.table.enabled>false</org.sagebionetworks.table.enabled>\
</properties></profile></profiles></settings>" > /var/lib/jenkins/${JOB_NAME}/.m2/settings.xml


# start up rds container
docker run --name ${rds_container_name} \
-e MYSQL_ROOT_PASSWORD=default-pw \
-e MYSQL_DATABASE=${rds_user_name} \
-e MYSQL_USER=${rds_user_name} \
-e MYSQL_PASSWORD=${rds_password} \
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
-Dorg.sagebionetworks.repository.database.connection.url=jdbc:mysql://${rds_container_name}/${rds_user_name} \
-DJDBC_CONNECTION_STRING=jdbc:mysql://${rds_container_name}/${rds_user_name} \
-Dorg.sagebionetworks.id.generator.database.connection.url=jdbc:mysql://${rds_container_name}/${rds_user_name} \
-Dorg.sagebionetworks.stackEncryptionKey=${org_sagebionetworks_stackEncryptionKey} \
-Dorg.sagebionetworks.stack.iam.id=${org_sagebionetworks_stack_iam_id} \
-Dorg.sagebionetworks.stack.iam.key=${org_sagebionetworks_stack_iam_key} \
-Dorg.sagebionetworks.stack.instance=${user} \
-Dorg.sagebionetworks.developer=${user} \
-Dorg.sagebionetworks.stack=${stack} \
-Dorg.sagebionetworks.table.enabled=false \
-Duser.home=/root"


clean_up_container ${build_container_name}

clean_up_container ${rds_container_name}

