from boto.s3.connection import S3Connection
from boto.s3.key import Key
from boto.connection import AWSAuthConnection

class SynapseAwsEnvironment:
    BeanstalkDefaultHost = 'elasticbeanstalk.us-east-1.amazon.com'
    
    def __init__(self, aws_access_key_id, aws_secret_access_key, bucketNameForWars, host=BeanstalkDefaultHost):
        '''
        connection = boto.s3.connection to the AWS account supporting a Synapse deployment
        bucketNameForWars = the name of the S3 bucket supporting the Elastic Beanstalk deployments
        '''
        self.s3connection = S3Connection(aws_access_key_id, aws_secret_access_key)
        self.bucket = self.s3connection.get_bucket(bucketNameForWars)
#        self.beanstalkConnection = AWSAuthConnection(host, aws_access_key_id, 
#                                                     aws_secret_access_key, is_secure=True, 
#                                                     port=None, proxy=None, proxy_port=None,
#                                                     proxy_user=None, proxy_pass=None,
#                                                     debug=0, https_connection_factory=None,path='/',
#                                                     provider='aws', security_token=None)
        
    def putFile(self, key, filename):
        k = Key(self.bucket)
        k.key = key
        k.set_contents_from_filename(filename)
        
    def getApplications(self):
        response = connection.make_request('GET', 
                'elasticbeanstalk.us-east-1.amazon.com', 
                None,
                None)
        body = response.read()
        
    def getAllBuckets(self):
        print self.connection.get_all_buckets()


        
