import artifactoryClient, synapseAwsEnvironment, os.path
from boto.s3.connection import S3Connection
from boto.s3.key import Key

# Environment constants
PLATFORM_DEPLOYMENT_BUCKET = 'elasticbeanstalk-us-east-1-325565585839'
MIKE_DEPLOYMENT_BUCKET = 'elasticbeanstalk-us-east-1-059816207990'
AUTH_SERVICE_WAR = 'services-authentication'
REPO_SERVICE_WAR = 'services-repository'
PORTAL_WAR = 'portal'
PROD_APPLICATION_NAME = 'Synapse'
STAGING_APPLICATION_NAME = 'Synapse-Staging'
DESCRIPTION = 'created by Synapse Deployer' 

class SynapseInstanceConfig:
    def __init__(self, application_name, auth_environment_name, 
                 repo_environment_name, portal_environment_name):
        self.application_name = application_name #Elastic beanstalk application name
        self.auth_environment_name = auth_environment_name #Beanstalk environment name for auth service, None if not updated
        self.repo_environment_name = repo_environment_name #Beanstalk environment name for repo service, None if not updated
        self.portal_environment_name = portal_environment_name #Beanstalk environment name for web portal, None if not updated

# Staging and Prod configs
STAGING_A_CONFIG = SynapseInstanceConfig(STAGING_APPLICATION_NAME, 'auth-staging-a', 'repo-staging-a', 'portal-staging-a')
STAGING_B_CONFIG = SynapseInstanceConfig(STAGING_APPLICATION_NAME, 'auth-staging-b', 'repo-staging-b', 'portal-staging-b')
STAGING_C_CONFIG = SynapseInstanceConfig(STAGING_APPLICATION_NAME, 'auth-staging-c', 'repo-staging-c', 'portal-staging-c')
PROD_A_CONFIG = SynapseInstanceConfig(PROD_APPLICATION_NAME, 'auth-prod-a', 'repo-prod-a', 'portal-prod-a')
PROD_B_CONFIG = SynapseInstanceConfig(PROD_APPLICATION_NAME, 'auth-prod-b', 'repo-prod-b', 'portal-prod-b')
PROD_C_CONFIG = SynapseInstanceConfig(PROD_APPLICATION_NAME, 'auth-prod-c', 'repo-prod-c', 'portal-prod-c')

# Parameters to drive Deployment - Change these as needed
deployment_bucket = PLATFORM_DEPLOYMENT_BUCKET
version = '0.10'
isSnapshot = True
workDir = os.sep + 'temp' + os.sep 
stacksToUpgrade = [STAGING_B_CONFIG, PROD_A_CONFIG]
componentsToUpgrade = [AUTH_SERVICE_WAR, REPO_SERVICE_WAR, PORTAL_WAR]


# Get the .wars to local work directory
artifacts = []
for component in componentsToUpgrade:
    artifact = artifactoryClient.downloadArtifact(component, version, isSnapshot, workDir)
    artifact.warName = component
    artifacts.append(artifact)
    
# Initialize environment
synapse = synapseAwsEnvironment.SynapseAwsEnvironment(deployment_bucket)

# For all wars we want to upgrade, put to S3, create beanstalk versions, and update beanstalk environments
for artifact in artifacts:
    key = synapse.putWar(artifact.fileName, artifact.warName, artifact.buildNumber, version, isSnapshot)
    version_label = key[0:len(key) - 4] #strip off .war
    print 'creating version ' + version_label
    for stack in stacksToUpgrade:
        synapse.createApplicationVersion(version_label, stack.application_name, key, description=DESCRIPTION)
        #synapse.updateEnvironment(environment_name = component.environment_name, version_label = version_label)
    
print('Done')

