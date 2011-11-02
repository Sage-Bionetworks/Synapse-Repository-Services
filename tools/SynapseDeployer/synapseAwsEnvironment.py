from boto.s3.connection import S3Connection
from boto.s3.key import Key
from boto.connection import AWSQueryConnection
from boto.ec2.connection import EC2Connection

class BeanstalkConnection(AWSQueryConnection):
    #For now this is only place elasticbeanstalk is supported
    BeanstalkDefaultHost = 'elasticbeanstalk.us-east-1.amazonaws.com'
    
    def __init__(self, aws_access_key_id=None, aws_secret_access_key=None, host=BeanstalkDefaultHost):
        #TODO: allow access key and id to optional, they may come in from config
        self.host = host
        AWSQueryConnection.__init__(self, aws_access_key_id, aws_secret_access_key, host=self.host)
        
    def _required_auth_capability(self):
        #HACK: Can same plug in work for beanstalk? Should really modify boto.
        return ['ec2'] 
    
    def getApplications(self):
        response = self.make_request('DescribeApplications')
        body = response.read()
        return body
    
    def updateEnvironment(self, description=None, environment_id=None, environment_name=None,
                          template_name=None, version_label=None):
        '''
        Updates an environment either with new configuration or new version, but not both on same call
        TODO: Support for changing individual option settings
        '''
        params = {}
        if description: params['Description']=description
        if environment_id: params['EnvironmentId']=environment_id
        if environment_name: params['EnvironmentName']=environment_name
        if template_name: params['TemplateName']=template_name
        if version_label: params['VersionLabel']=version_label
        self.make_request('UpdateEnvironment', params)
    
    def createApplicationVersion(self, version_label, application_name, 
                                 source_bucket, source_key, description=None, 
                                 autocreate_application=False):
        params = {}
        params['VersionLabel'] = version_label
        params['SourceBundle.S3Bucket'] = source_bucket
        params['SourceBundle.S3Key'] = source_key
        params['ApplicationName'] = application_name
        if not autocreate_application:
            params['AutoCreateApplication'] = 'true'
        if description:
            params['Description'] = description
        self.make_request('CreateApplicationVersion', params)
    
    def make_request(self, action, params=None, path='/', verb='GET'):
        #Modifying the equivalent method in ec2Connection as beanstalk uses 'Operation' instead of 'Action'
        http_request = self.build_base_http_request(verb, path, None,
                                                    params, {}, '',
                                                    self.server_name())
        if action:
            http_request.params['Operation'] = action
        #http_request.params['Version'] = self.APIVersion #Seems not needed
        return self._mexe(http_request)

class SynapseAwsEnvironment:
# This class provides a facade to the various boto connection classes 
# that each represent a different AWS service

    def __init__(self, bucketNameForWars, aws_access_key_id=None, aws_secret_access_key=None):
        '''
        aws_access_key_id = aws account id, if None will come from config file
        aws_secret_access_key = aws account password, if None will come from config file
        bucketNameForWars = the name of the S3 bucket supporting the Elastic Beanstalk deployments
        '''
        self.s3connection = S3Connection(aws_access_key_id, aws_secret_access_key)
        self.bucket_name = bucketNameForWars
        self.bucket = self.s3connection.get_bucket(bucketNameForWars)
        self.beanstalkConnection = BeanstalkConnection(aws_access_key_id, aws_secret_access_key)
        self.ec2Connection = EC2Connection(aws_access_key_id, aws_secret_access_key)
    
    def putWar(self, fileName, artifactName, buildNumber, version, isSnapshot):
        key = artifactName+'-'+version
        if isSnapshot: key += '-SNAPSHOT'
        key += '-'+str(buildNumber)+'.war'
        print('uploading '+key)
        self.putFile(key, fileName)
        return key
        
    def putFile(self, key, filename):
        k = Key(self.bucket)
        k.key = key
        k.set_contents_from_filename(filename)
        
    def getApplications(self):
        response = self.beanstalkConnection.getApplications()
        print response
        
    def getInstances(self):
        response = self.ec2Connection.get_all_instances()
        print response
        
    def getAllBuckets(self):
        print self.s3connection.get_all_buckets()
        
    def createApplicationVersion(self, version_label, application_name, 
                                 source_key, description=None):
        response = self.beanstalkConnection.createApplicationVersion(version_label, 
                                                                     application_name, 
                                                                     self.bucket_name, 
                                                                     source_key, 
                                                                     description)
        print response
    
    def updateEnvironment(self, description=None, environment_id=None, environment_name=None,
                          template_name=None, version_label=None):
        response = self.beanstalkConnection.updateEnvironment(description, environment_id, environment_name, template_name, version_label)
        print response