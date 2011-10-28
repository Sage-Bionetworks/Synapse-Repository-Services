import sageArtifactory, synapseAwsEnvironment
from boto.s3.connection import S3Connection
from boto.s3.key import Key

# Parameters to drive Deployment

# Connection to AWS account DON'T CHECK PASSWORDS IN TO SVN
account_id = 'xxx'
secret_key = 'yyyy'

# Change to match beanstalk bucket for your account
deploymentBucket = 'elasticbeanstalk-us-east-1-059816207990'

workDir = '/temp/' 
warsToUpgrade = sageArtifactory.SUPPORTED_ARTIFACT_NAMES #all of them
version = '0.8'
isSnapshot = True

synapse = synapseAwsEnvironment.SynapseAwsEnvironment(account_id, secret_key, deploymentBucket)
#Testing connection to s3 
#synapse.getAllBuckets()

for war in warsToUpgrade:
    artifact = sageArtifactory.downloadArtifact(war, version, isSnapshot, workDir)
    key = war+'-'+version
    if isSnapshot: key += '-SNAPSHOT'
    key += '-'+str(artifact.buildNumber)+'.war'
    print('uploading '+key)
    synapse.putFile(key, artifact.fileName)

    
print('Done')

