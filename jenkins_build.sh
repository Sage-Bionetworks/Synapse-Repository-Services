#!/bin/bash
# The required environment variables are:
# user - e.g. 'pjmhill'
# org_sagebionetworks_stack_iam_id - the id of the developer's AWS secret key
# org_sagebionetworks_stack_iam_key - the developer's AWS secret key
# org_sagebionetworks_stackEncryptionKey - the stack encryption key, common to all dev builds
# rds_password - the password for the build database, common to all dev builds

# On Jenkins HOME is /var/lib/jenkins
export m2_cache_parent_folder=${HOME}/${JOB_NAME}
export src_folder=${HOME}/workspace/${JOB_NAME}

# Now call the more generic 'docker_build.sh'
BASEDIR=$(dirname "$0")

export stack=dev

${BASEDIR}/before.sh
${BASEDIR}/docker_build.sh
export result=$?
${BASEDIR}/after.sh
exit ${result}