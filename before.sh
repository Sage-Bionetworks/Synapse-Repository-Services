#!/bin/bash
# The required environment variables are:
# user - e.g. 'pjmhill'

# optional variables for github commit status API
# github_token - the token that is used to push commit status
# Instructions: https://sagebionetworks.jira.com/wiki/display/PLFM/Developer+Bootstrap#DeveloperBootstrap-PushPrivateJenkinsBuildStatustoGithubasCommitStatus

# push PENDING status to github
if [[ ${github_token} ]] 
then
  curl "https://api.github.com/repos/Sage-Bionetworks/Synapse-Repository-Services/statuses/$GIT_COMMIT" \
    -H "Authorization: token ${github_token}" \
    -H "Content-Type: application/json" \
    -X POST \
    -d "{\"state\": \"pending\", \"description\": \"Jenkins\", \"target_url\": \"http://build-system-synapse.sagebase.org:8081/job/${stack}${user}/$BUILD_NUMBER/console\"}"
fi
