from boto.s3.connection import S3Connection
from boto.s3.key import Key
from exception import SynapseDeployerError

class SynapseAwsEnvironment:
    def __init__(self, connection, bucketNameForWars):
        '''
        connection = boto.s3.connection to the AWS account supporting a Synapse deployment
        bucketNameForWars = the name of the S3 bucket supporting the Elastic Beanstalk deployments
        '''
        self.connection = connection
        self.bucket = connection.get_bucket(bucketNameForWars)
        
    def putFile(self, key, filename):
        k = Key(self.bucket)
        k.key = key
        k.set_contents_from_filename(filename)
        
    


        
