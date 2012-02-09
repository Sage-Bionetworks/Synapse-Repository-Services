'''
Created on Sep 23, 2011
    # This script is doing one maintenance task: fetching .war files for a Synapse deployment.
    # This is the place for any Sage-Artifactory specific code. 
    # Will eventually be a function in more complete module for Synapse Administration 

@author: mkellen
'''
import json, synapse.utils, zipfile, urllib
from exception import SynapseDeployerError

SUPPORTED_ARTIFACT_NAMES = ["portal", "services-repository", "services-authentication"]

class Artifact:
    '''
    Describes an artifact obtained from Artifactory
    '''
    def __init__(self,fileName,buildNumber,warName=None):
        self.fileName = fileName
        self.buildNumber = buildNumber

def _determineDownloadURLForResource(artifactName, version, isSnapshot):
    path = "http://sagebionetworks.artifactoryonline.com/sagebionetworks/"
    return _determineDownloadURL(artifactName, version, isSnapshot, path)
    
def _determineDownloadURLForMetadata(artifactName, version, isSnapshot):
    path = "http://sagebionetworks.artifactoryonline.com/sagebionetworks/api/storage/"
    return _determineDownloadURL(artifactName, version, isSnapshot, path) + ':sample-metadata'

def _determineDownloadURL(artifactName, version, isSnapshot, path):  
    if isSnapshot:
        path += "libs-snapshots-local"
    else:
        path += "libs-releases-local"
    path += "/org/sagebionetworks/"
    if (SUPPORTED_ARTIFACT_NAMES.__contains__(artifactName)):
        path += artifactName
    else:
        raise SynapseDeployerError, "Error: unrecognized module"
    path += "/"
    path += version
    if isSnapshot:
        path += "-SNAPSHOT"
    path += "/" + artifactName + "-" + version
    if isSnapshot:
        path += "-SNAPSHOT"
    path += ".war" 
    return path

def _findBuildNumber(fileName):
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

def downloadArtifact(moduleName, version, isSnapshot, workDir):
    '''
    Get an artifact from Artifactory and verify download worked via MD5
    '''
    
    # Get the war file and check it's MD5
    warUrl = _determineDownloadURLForResource(moduleName, version, isSnapshot)
    print('preparing to download from ' + warUrl)
    tempFileName = workDir + moduleName +'-'+ version+ '.war'
    urllib.urlretrieve(warUrl, tempFileName)
    md5 = synapse.utils.computeMd5ForFile(tempFileName)

    # Get the metadata and see what actual MD5 should be
    metaUrl = _determineDownloadURLForMetadata(moduleName, version, isSnapshot)
    metadataFileName = workDir + moduleName + '.json'
    urllib.urlretrieve(metaUrl, metadataFileName)
    file = open(metadataFileName, 'r')
    metadata = json.load(file)
    file.close()
    expectedMD5hexdigest = metadata["checksums"]["md5"]

    if (md5.hexdigest() == expectedMD5hexdigest):
        print("Downloads completed successfully")
    else:
        raise SynapseDeployerError, "ERROR: MD5 does not match"

    buildNumber = _findBuildNumber(tempFileName)
    return Artifact(tempFileName, buildNumber)

#------- UNIT TESTS -----------------
if __name__ == '__main__':
    import unittest

    class TestSageArtifactory(unittest.TestCase):
        
        def test_getArtifact(self):
            #Test happy case with a small artifact
            artifact = downloadArtifact('services-authentication', '0.7.9', False, '/temp/')
            self.assertEqual(4655, artifact.buildNumber)
            self.assertEqual('/temp/services-authentication-0.7.9.war', artifact.fileName)
        
        def test_getSnapshot(self):
            #Just test that the paths here are right
            result = _determineDownloadURLForResource('services-authentication', '0.7', True)
            self.assertEqual(result, 'http://sagebionetworks.artifactoryonline.com/sagebionetworks/libs-snapshots-local/org/sagebionetworks/services-authentication/0.7-SNAPSHOT/services-authentication-0.7-SNAPSHOT.war')
            
        def test_getUnsupported(self):
            #Test exception raising
            self.assertRaisesRegexp(SynapseDeployerError, "Error: unrecognized module", _determineDownloadURLForMetadata,'unknown', '0.7', True)
                
    unittest.main()