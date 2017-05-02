#!/bin/bash
# The required environment variables are:
# user - e.g. 'pjmhill'

# optional variables for github commit status API
# github_token - the token that is used to push commit status
# Instructions: https://sagebionetworks.jira.com/wiki/display/PLFM/Developer+Bootstrap#DeveloperBootstrap-PushPrivateJenkinsBuildStatustoGithubasCommitStatus

# require to check status
# result - the result of the docker_build.sh

# map Jenkins status to github commit status
if [[ $result -eq 0 ]]
then
  export STATUS="success"
else
  export STATUS="failure"
fi

# push build status to github
if [[ ${github_token} ]] 
then
  curl "https://api.github.com/repos/Sage-Bionetworks/Synapse-Repository-Services/statuses/$GIT_COMMIT" \
    -H "Authorization: token ${github_token}" \
    -H "Content-Type: application/json" \
    -X POST \
    -d "{\"state\": \"$STATUS\", \"description\": \"Jenkins\", \"target_url\": \"http://build-system-synapse.sagebase.org:8081/job/${stack}${user}/$BUILD_NUMBER/console\"}"
fi