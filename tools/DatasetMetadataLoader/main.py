import sageArtifactory, synapseAwsEnvironment
from boto.s3.connection import S3Connection
from boto.s3.key import Key

# Parameters to drive Deployment

# Connection to Mike's account DON'T CHECK THIS IN
conn = S3Connection('account id','secret key')
# Change to match platform bucket
deploymentBucket = 'elasticbeanstalk-us-east-1-059816207990'

workDir = '/temp/' 
warsToUpgrade = sageArtifactory.SUPPORTED_ARTIFACT_NAMES #all of them
version = '0.8'
isSnapshot = True

synapse = synapseAwsEnvironment.SynapseAwsEnvironment(conn, deploymentBucket)

for war in warsToUpgrade:
    artifact = sageArtifactory.downloadArtifact(war, version, isSnapshot, workDir)
    key = war+'-'+version
    if isSnapshot: key += '-SNAPSHOT'
    key += '-'+str(artifact.buildNumber)+'.war'
    print('uploading '+key)
    synapse.putFile(key, artifact.fileName)
    
    
print('Done')

