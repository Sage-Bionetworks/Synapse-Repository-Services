'''
Created on Sep 23, 2011
    # COMMENT: This script is doing one maintenance task: updating .war files in place for a stack.  
    # Will eventually be a function in more complete module for Synapse Administration 
@author: mkellen
'''
import urllib2, json, synapse.utils, hashlib, zipfile

#QUESTION: Better way to define global / module constants?  Equivalent of Java final?
SUPPORTED_ARTIFACT_NAMES = ("portal", "services-repository", "services-authentication")

def determineDownloadURLForResource(artifactName, version, isSnapshot):
    path = "http://sagebionetworks.artifactoryonline.com/sagebionetworks/"
    return determineDownloadURL(artifactName, version, isSnapshot, path)
    
def determineDownloadURLForMetadata(artifactName, version, isSnapshot):
    path = "http://sagebionetworks.artifactoryonline.com/sagebionetworks/api/storage/"
    return determineDownloadURL(artifactName, version, isSnapshot, path) + ':sample-metadata'

def determineDownloadURL(artifactName, version, isSnapshot, path):  
    #QUESTION: How do I make this function "private" in Python?  
    if isSnapshot:
        path += "libs-snapshots-local"
    else:
        path += "libs-releases-local"
    path += "/org/sagebionetworks/"
    if (SUPPORTED_ARTIFACT_NAMES.__contains__(artifactName)):
        path += artifactName
    else:
        print("Error: unrecognized module")
        return None
    path += "/"
    path += version
    if isSnapshot:
        path += "-SNAPSHOT"
    path += "/" + artifactName + "-" + version
    if isSnapshot:
        path += "-SNAPSHOT"
    path += ".war" 
    return path

def downloadFile(url, tempFileName):
    '''
    Download a file from the internet and stream to filesystem, one CHUNK at a time
    '''
    #TODO: This function should probably move into Syanapse Utils.
    req = urllib2.urlopen(url)
    CHUNK = 16 * 1024
    with open(tempFileName, 'wb') as file: #wb is write binary
        while True:
            chunk = req.read(CHUNK)
            if not chunk: break
            file.write(chunk)

def findBuildNumber(fileName):
    '''
    Crack open the War and pull the build number out of the manifest file
    '''
    zipFile = zipfile.ZipFile(fileName, 'r')
    mf = zipFile.open('META-INF/MANIFEST.MF', 'r')
    line = mf.readline()
    buildNumber = None
    while (line != None):
        if (line.startswith('Implementation-Build')):
            buildNumber = line.partition(':')[2]
            buildNumber = buildNumber.lstrip().rstrip()
            break
        line = mf.readline()
    zipFile.close()
    return int(buildNumber)

def downloadArtifact(moduleName, version, isSnapshot):
    '''
    Get an artifact from Artifactory and verify download worked via MD5
    '''
    
    # Get the war file and check it's MD5
    warUrl = determineDownloadURLForResource(moduleName, version, isSnapshot)
    print('preparing to download from ' + warUrl)
    tempFileName = '/temp/' + moduleName +'-'+ version+ '.war'
    downloadFile(warUrl, tempFileName)
    md5 = synapse.utils.computeMd5ForFile(tempFileName)

    # Get the metadata and see what actual MD5 should be
    # COMMENT - Could just pull file into memory, but this is easy for debugging for now
    metaUrl = determineDownloadURLForMetadata(moduleName, version, isSnapshot)
    print('preparing to download from ' + metaUrl)
    metadataFileName = '/temp/' + moduleName + '.json'
    downloadFile(metaUrl, metadataFileName)
    file = open(metadataFileName, 'r')
    metadata = json.load(file)
    file.close()
    expectedMD5hexdigest = metadata["checksums"]["md5"]

    if (md5.hexdigest() == expectedMD5hexdigest):
        print("Downloads completed successfully")
    else:
    # COMMENT: Need to learn right way to handle exceptions in Python
        print("ERROR: MD5 does not match")

    return findBuildNumber(tempFileName)

def downloadAll(version, isSnapshot):
    # Get all artifacts
    buildNumber = 0
    for artifactName in SUPPORTED_ARTIFACT_NAMES:
        buildNumber = downloadArtifact(artifactName, version, isSnapshot)
        print 'build number '+buildNumber
    return buildNumber
        

#------- UNIT TESTS -----------------
if __name__ == '__main__':
    import unittest

    class TestSageArtifactory(unittest.TestCase):
        
        def test_getArtifact(self):
            #Test happy case with a small artifact
            #TODO: Find a smaller artifact to speed this up
            buildNumber = downloadArtifact('services-authentication', '0.7.9', False)
            self.assertEqual(4655, buildNumber)
            
    unittest.main()