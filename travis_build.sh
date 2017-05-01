#!/bin/bash
# The required environment variables are:
# user - e.g. 'pjmhill'
# org_sagebionetworks_stack_iam_id - the id of the developer's AWS secret key
# org_sagebionetworks_stack_iam_key - the developer's AWS secret key
# org_sagebionetworks_stackEncryptionKey - the stack encryption key, common to all dev builds
# rds_password - the password for the build database, common to all dev builds

# current dir
export origin_pwd=${PWD}

# Now call the more generic 'docker_build.sh'
${PWD}/docker_build.sh
