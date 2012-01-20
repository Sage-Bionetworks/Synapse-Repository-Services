import artifactoryClient, synapseAwsEnvironment, os.path
from boto.s3.connection import S3Connection
from boto.s3.key import Key

class Component:
    def __init__(self, environment_name, artifact_name):
        self.environment_name = environment_name
        self.artifact_name = artifact_name

# Parameters to drive Deployment
workDir = os.sep+'temp'+os.sep 
componentsToUpgrade = [Component('prod-c-auth', 'services-authentication'),
                       Component('prod-c-repo', 'services-repository'),
                       Component('prod-c-portal', 'portal')]
version = '0.9'
isSnapshot = True
beanstalk_application_name = 'Synapse'
description = 'created by Synapse Deployer' 

# Change to match beanstalk bucket for your account
#Mike
#deploymentBucket = 'elasticbeanstalk-us-east-1-059816207990'
# Platform
deploymentBucket = 'elasticbeanstalk-us-east-1-325565585839'

# Initialize environment
synapse = synapseAwsEnvironment.SynapseAwsEnvironment(deploymentBucket)

# For all wars we want to upgrade, download from artifactory, put to S3, create beanstalk versions, and update beanstalk environments
for component in componentsToUpgrade:
    artifact = artifactoryClient.downloadArtifact(component.artifact_name, version, isSnapshot, workDir)
    key = synapse.putWar(artifact.fileName, component.artifact_name, artifact.buildNumber, version, isSnapshot)
    version_label = key[0:len(key)-4] #strip off .war
    print 'creating version '+version_label
    synapse.createApplicationVersion(version_label, beanstalk_application_name, 
                                     key, description=description)
    synapse.updateEnvironment(environment_name = component.environment_name, version_label = version_label)
    
print('Done')

