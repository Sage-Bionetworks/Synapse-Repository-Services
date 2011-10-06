import sageArtifactory

# Main entry
sageArtifactory.downloadAll('0.7.9', False)


# upload to S3
'''
s3url = 'https://s3.amazonaws.com/elasticbeanstalk-us-east-1-325565585839/portal-0.7-SNAPSHOT-4544.war'+moduleName+'-'+version+'-'
if isSnapshot:
    s3url += 'SNAPSHOT-'
s3url += buildNumber + '.war'
synapse.utils.uploadToS3(tempFileName, s3url, md5, 'application/x-zip', True)
print("Upload to S3 completed successfully")
'''