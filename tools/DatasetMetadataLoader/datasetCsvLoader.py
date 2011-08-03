#!/usr/bin/env python2.7

# To debug this, python -m pdb myscript.py

import os, csv, json, re, string, datetime, urllib, httplib, ConfigParser, itertools, argparse, codecs, cStringIO, synapse.client, synapse.utils

#-------[ Documentation embedded in Command Line Arguments ]----------------
parser = synapse.utils.createBasicArgParser('Tool to load metadata into a Sage Platform Repository Service.  Note that this script always create instance of a dataset in the repository service (the repository service does not enforce uniqueness of dataset names).  Use the datasetNuker.py script first if you want to start with a clean datastore.')

synapse.client.addArguments(parser)

parser.add_argument('--datasetsCsv', '-d', help='the file path to the CSV file holding dataset metadata, defaults to AllDatasets.csv', default='AllDatasets.csv')

parser.add_argument('--layersCsv', '-l', help='the file path to the CSV file holding layer metadata, defaults to AllDatasetLayerLocations.csv', default='AllDatasetLayerLocations.csv')

parser.add_argument('--fakeLocalData', '-f', help='use fake data when we would normally read something from the actual Sage Bionetworks datasets, defaults to False', action='store_true', default=False)

parser.add_argument('--uploadData', '-3', help='upload datasets to S3, defaults to False', action='store_true', default=False)

#-------------------[ Constants ]----------------------

NOW = datetime.datetime.today()

SOURCE_DATA_DIRECTORY = '/work/platform/source'

# These are the fields in the CSV that correspond to primary fields in
# our data model, all other fields in the CSV will be tossed into the
# annotation buckets
CSV_TO_PRIMARY_FIELDS = {
    'name': 'name',
    'description': 'description',
    'Investigator': 'creator',
    'Creation Date': 'creationDate',
    'Status': 'status',
    'date_released': 'releaseDate',
    'version':'version'
    }

CSV_SKIP_FIELDS = ["db_id","user_agreement_file_path", "readme_file_path"];
CSV_LOCATION_FIELDS = ["ds_location_awss3"]

SAGE_CURATION_PROJECT_NAME = "SageBioCuration"
SAGE_CURATION_EULA_NAME = "SageBioCurationEula"

ROOT_PERMS = {
    "Sage Curators":["READ","CHANGE_PERMISSIONS","DELETE","UPDATE","CREATE"],
#    "AUTHENTICATED_USERS":["READ"],
    "PUBLIC":["READ"]
    }

LOCATION_PERMS = {
    "Sage Curators":["READ","CHANGE_PERMISSIONS","UPDATE","CREATE"],
    "AUTHENTICATED_USERS":["READ"]
    }

DEFAULT_TERMS_OF_USE = "<p><b><larger>Copyright 2011 Sage Bionetworks</larger></b><br/><br/></p><p>Licensed under the Apache License, Version 2.0 (the \"License\"). You may not use this file except in compliance with the License. You may obtain a copy of the License at<br/><br/></p><p>&nbsp;&nbsp;<a href=\"http://www.apache.org/licenses/LICENSE-2.0\" target=\"new\">http://www.apache.org/licenses/LICENSE-2.0</a><br/><br/></p><p>Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an \"AS IS\" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions andlimitations under the License.<br/><br/></p><p><strong><a name=\"definitions\">1. Definitions</a></strong>.<br/><br/></p> <p>\"License\" shall mean the terms and conditions for use, reproduction, and distribution as defined by Sections 1 through 9 of this document.<br/><br/></p> <p>\"Licensor\" shall mean the copyright owner or entity authorized by the copyright owner that is granting the License.<br/><br/></p> <p>\"Legal Entity\" shall mean the union of the acting entity and all other entities that control, are controlled by, or are under common control with that entity. For the purposes of this definition, \"control\" means (i) the power, direct or indirect, to cause the direction or management of such entity, whether by contract or otherwise, or (ii) ownership of fifty percent (50%) or more of the outstanding shares, or (iii) beneficial ownership of such entity.<br/><br/></p> <p>\"You\" (or \"Your\") shall mean an individual or Legal Entity exercising permissions granted by this License.<br/><br/></p> <p>\"Source\" form shall mean the preferred form for making modifications, including but not limited to software source code, documentation source, and configuration files.<br/><br/></p> <p>\"Object\" form shall mean any form resulting from mechanical transformation or translation of a Source form, including but not limited to compiled object code, generated documentation, and conversions to other media types.<br/><br/></p> <p>\"Work\" shall mean the work of authorship, whether in Source or Object form, made available under the License, as indicated by a copyright notice that is included in or attached to the work (an example is provided in the Appendix below).<br/><br/></p> <p>\"Derivative Works\" shall mean any work, whether in Source or Object form, that is based on (or derived from) the Work and for which the editorial revisions, annotations, elaborations, or other modifications represent, as a whole, an original work of authorship. For the purposes of this License, Derivative Works shall not include works that remain separable from, or merely link (or bind by name) to the interfaces of, the Work and Derivative Works thereof.<br/><br/></p> <p>\"Contribution\" shall mean any work of authorship, including the original version of the Work and any modifications or additions to that Work or Derivative Works thereof, that is intentionally submitted to Licensor for inclusion in the Work by the copyright owner or by an individual or Legal Entity authorized to submit on behalf of the copyright owner. For the purposes of this definition, \"submitted\" means any form of electronic, verbal, or written communication sent to the Licensor or its representatives, including but not limited to communication on electronic mailing lists, source code control systems, and issue tracking systems that are managed by, or on behalf of, the Licensor for the purpose of discussing and improving the Work, but excluding communication that is conspicuously marked or otherwise designated in writing by the copyright owner as \"Not a Contribution.\"<br/><br/></p> <p>\"Contributor\" shall mean Licensor and any individual or Legal Entity on behalf of whom a Contribution has been received by Licensor and subsequently incorporated within the Work.<br/><br/></p>"

#-------------------[ Global Variables ]----------------------

# Command line arguments
gARGS = {}
gARGS = parser.parse_args()
gSYNAPSE = synapse.client.factory(gARGS)

# A mapping we build over time of dataset names to dataset ids.  In our
# layer CSV file we have the dataset name to which each layer belongs.
gDATASET_NAME_2_ID = {}

def checkEmptyRepository():
    """
    Helper function to check that repository is empty.
    Also used to load the groups as side-effect.
    """
    # Postpone until bugfix
    #chkList = ["/project", "/dataset", "/layer", "/preview", "/location"]
    #for c in chkList:
    #    l = gSYNAPSE.getRepoEntity(c)
    #    if 0 == len(l):
    #        return False
    return True

def createAccessList(principals, permissionList):
    """
    Helper function to return access list from list of principals
    """
    al = []
    for p in principals:
        if p["name"] in permissionList:
#            al.append({"userGroupId":p["id"], "accessType":permissionList[p["name"]]})
            al.append({"groupName":p["name"], "accessType":permissionList[p["name"]]})
        #print "principal %s \t access list %s" % (p, al)
    return al

def createOrUpdateEntity(kind, entity, permissions=None):
    """
    Helper function to query to determine whether the entity exists
    and if so updated instead of create the entity.  Note that
    different entities need different queries to find the unique
    instance.

    Note that permissions defaults to None meaning that this entity
    should just inherit the permissions of its parent.    
    """
    if(("location" == kind) or ("preview" == kind)):
        storedEntity = gSYNAPSE.getRepoEntityByProperty(kind=kind,
                                                        propertyName="parentId",
                                                        propertyValue=entity['parentId'])
        message = " for " + entity['parentId']
    elif("layer" == kind):
        storedEntity = gSYNAPSE.getRepoEntityByProperty(kind=kind,
                                                        propertyName="name",
                                                        propertyValue=entity['name'],
                                                        parentId=entity['parentId'])
        message = entity['name'] + " for " + entity['parentId']
    else:
        if(1 > len(entity['name'])):
            raise Exception("entity must have a valid name") 
        # If the repo svc has no schema, this will throw, when
        # http://sagebionetworks.jira.com/browse/PLFM-226 is done we can
        # remove this try/catch
        try:
            storedEntity = gSYNAPSE.getRepoEntityByName(kind, entity['name'])
            message = entity['name']
        except Exception, err:
            storedEntity = None
            message = entity['name']
    
    if(None == storedEntity):
        storedEntity = gSYNAPSE.createRepoEntity("/" + kind, entity)
        if(None != permissions):
            accessList = createAccessList(gSYNAPSE.getPrincipals(),
                                          permissions)
#            acl = {"resourceAccess":accessList, "resourceId":storedEntity["id"]}
            acl = {"resourceAccess":accessList}
            if(not('parentId' in storedEntity) or
               (None == storedEntity['parentId'])):
                gSYNAPSE.updateRepoEntity(storedEntity["accessControlList"], acl)
            else:
                gSYNAPSE.createRepoEntity(storedEntity["accessControlList"], acl)
        print 'Created %s %s\n\n' % (kind, message)
    else:
        storedEntity = gSYNAPSE.updateRepoEntity(storedEntity["uri"], entity)
        if(None != permissions):
            accessList = createAccessList(gSYNAPSE.getPrincipals(),
                                          permissions)
#            acl = {"resourceAccess":accessList, "resourceId":storedEntity["id"]}
            acl = {"resourceAccess":accessList}
            gSYNAPSE.updateRepoEntity(storedEntity["accessControlList"], acl)
        print 'Updated %s %s\n\n' % (kind, message)

    return storedEntity

def createOrUpdateDataset(dataset, annotations, location):
    """
    Helper function to create or update a dataset, its annotations,
    and its location as appropriate.
    """
    storedDataset = createOrUpdateEntity(kind="dataset", entity=dataset)
        
    # Put our annotations
    gSYNAPSE.updateRepoEntity(storedDataset["annotations"], annotations)

    # If there's a dataset location, set its parentId to created
    # dataset id and add location
    if None != location:
        # Cannot create orphan location
        location["parentId"] = storedDataset["id"]
        createOrUpdateLocation(location=location)

    # Stash the dataset id for later use
    gDATASET_NAME_2_ID[dataset['name']] = storedDataset['id']
      
def createOrUpdateLocation(location):
    """
    Helper method to create or update a location and optionally upload
    the data to S3
    """
    if 0 != string.find(location['path'], "/"):
        location["path"] = "/" + location["path"]

    md5 = None
    if(gARGS.fakeLocalData):
        location["md5sum"] = '0123456789ABCDEF0123456789ABCDEF'
    else:
        md5 = synapse.utils.computeMd5ForFile(SOURCE_DATA_DIRECTORY + location['path'])
        location["md5sum"] = md5.hexdigest()

    storedLocation = createOrUpdateEntity(kind="location",
                                          entity=location,
                                          permissions=LOCATION_PERMS)

    if(not gARGS.fakeLocalData and gARGS.uploadData):
        # TODO skip uploads for files if the checksum has not changed
        # TODO spawn a thread for each upload and proceed to get more throughput
        ## 20110715, migration to bucket devdata01, skip this dataset since its laready there
        #if('/mskcc_prostate_cancer.zip' == location['path']):
        #    return

        localFilepath = SOURCE_DATA_DIRECTORY + location['path']
        synapse.utils.uploadToS3(localFilepath=localFilepath,
                                 s3url=storedLocation["path"],
                                 md5=md5,
                                 debug=gARGS.debug)
        
#--------------------[ loadDatasets ]-----------------------------
# What follows is code that expects a dataset CSV in a particular format,
# sorry its so brittle and ugly
def loadDatasets(projectId, eulaId):
    # xschildw: Use codecs.open and UnicodeReader class to handle extended chars
    ifile  = open(gARGS.datasetsCsv, "r")
    reader = synapse.utils.UnicodeReader(ifile, encoding='latin_1')

    # loop variables
    rownum = -1
    previousDatasetId = None;
    # per dataset variables
    colnum = 0
    dataset = {}
    annotations = {}
    stringAnnotations = {}
    doubleAnnotations = {}
    longAnnotations = {}
    dateAnnotations = {}
    annotations['stringAnnotations'] = stringAnnotations
    annotations['doubleAnnotations'] = doubleAnnotations
    annotations['longAnnotations'] = longAnnotations
    annotations['dateAnnotations'] = dateAnnotations
    stringAnnotations['Tissue_Tumor'] = []
    
    for row in reader:
        rownum += 1

        # Save header row
        if rownum == 0:
            header = row
            colnum = 0
            for col in row:
                # Replace all runs of whitespace with a single dash
                header[colnum] = re.sub(r"\s+", '_', col)
                colnum += 1
            continue

        # Bootstrap our previousDatasetId
        if(None == previousDatasetId):
            previousDatasetId = row[0]
    
        # If we have read in all the data for a dataset, send it
        if(previousDatasetId != row[0]):
            # Create our dataset
            createOrUpdateDataset(dataset=dataset,
                                  annotations=annotations,
                                  location=location)
            # Re-initialize per dataset variables
            previousDatasetId = row[0]
            dataset = {}
            annotations = {}
            stringAnnotations = {}
            doubleAnnotations = {}
            longAnnotations = {}
            dateAnnotations = {}
            annotations['stringAnnotations'] = stringAnnotations
            annotations['doubleAnnotations'] = doubleAnnotations
            annotations['longAnnotations'] = longAnnotations
            annotations['dateAnnotations'] = dateAnnotations
            stringAnnotations['Tissue_Tumor'] = []
                        
        # Load the row data from the dataset CSV into our datastructure    
        colnum = 0
        for col in row:
            if(gARGS.debug):
                print '%-8s: %s' % (header[colnum], col)
            if(header[colnum] in CSV_TO_PRIMARY_FIELDS):
                if("name" == header[colnum]):
                    cleanName = col.replace("_", " ")
                    dataset[CSV_TO_PRIMARY_FIELDS[header[colnum]]] = cleanName
                else:
                    dataset[CSV_TO_PRIMARY_FIELDS[header[colnum]]] = col
            elif(header[colnum] in CSV_SKIP_FIELDS):
                if(gARGS.debug):
                    print 'SKIPPING %-8s: %s' % (header[colnum], col)
                # TODO consider reading these into fields
                #             user_agreement_file_path
                #             readme_file_path
            # TODO: Add code to handle whole dataset file location here
            elif(header[colnum] in CSV_LOCATION_FIELDS):
                if(gARGS.debug):
                    print 'Processing dataset location column'
                location = None
                if "NA" != col:
                    path = col
                    location = {}
                    location["type"] = "awss3"
                    location["path"] = path
            else:
                if( re.search('date', string.lower(header[colnum])) ):
                    ## TODO: Fix data file and remove following code
                    #d = ""
                    #print col
                    #m = re.match("((\d+)/(\d+)/(\d\d\d\d))", col)
                    #if None != m.group(0):
                    #    d = m.group(3) + '-' + m.group(2) + '-' + m.group(1)
                    ## End code to remove
                    dateAnnotations[header[colnum]] = [col]
                else:
                    try:
                        value = float(col)
                        if(value.is_integer()):
                            longAnnotations[header[colnum]] = [value]
                        else:
                            doubleAnnotations[header[colnum]] = [value]
                    except (AttributeError, ValueError):
                        # Note that all values in the spreadsheet from the
                        # mysql db are single values except for this one
                        if("Tissue/Tumor" == header[colnum]): 
                            stringAnnotations['Tissue_Tumor'].append(col)
                        else:
                            stringAnnotations[header[colnum]] = [col]
            colnum += 1
        dataset["parentId"] = projectId
        dataset["eulaId"] = eulaId
    ifile.close()     

    # Send the last one, create our dataset
    createOrUpdateDataset(dataset=dataset,
                          annotations=annotations,
                          location=location)

#--------------------[ loadLayers ]-----------------------------
def loadLayers():
    # What follows is code that expects a layerCsv in a particular format,
    # sorry its so brittle and ugly
    ifile  = open(gARGS.layersCsv, "r")
    reader = synapse.utils.UnicodeReader(ifile, encoding='latin_1')
    rownum = -1
    for row in reader:
        rownum += 1

        if rownum == 0:
            # Save header row
            header = row
            continue
        
        # xschildw: new format is
        # Dataset Name,type,status,name,Number of samples,Platform,Version,preview,sage,awsebs,awss3,qcby
        colnum = 0
        layer = {}
        layer["parentId"] = gDATASET_NAME_2_ID[row[0]]
        layer["type"] = row[1]
        layer["status"] = row[2]
        layer["name"] = row[3]
        layer["numSamples"] = row[4]
        layer["platform"] = row[5]
        layer["version"] = row[6]
        layer["qcBy"] = row[11]
        
        newLayer = createOrUpdateEntity(kind="layer", entity=layer)
        if newLayer == None:
            raise Exception("ENTITY_CREATION_ERROR")
        
        # Ignore column 8 (sage loc) and 9 (awsebs loc) for now
        for col in [10]:
            if(row[col] != ""):
                # trim whitespace off both sides
                path = row[col].strip()
                location = {}
                location["parentId"] = newLayer["id"]   # Cannot create orphaned location
                location["type"] = header[col]
                location["path"] = path
                createOrUpdateLocation(location=location)
        
        layerPreview = {}
        
        if(row[7] != ""):
            layerPreview["parentId"] = newLayer["id"]
            if(gARGS.fakeLocalData):
                layerPreview["previewString"] = 'this\tis\ta\tfake\tpreview\nthis\tis\ta\tfake\tpreview\n'
            else:
                with open(row[7]) as myfile:
                    # Slurp in the first six lines of the file and store
                    # it in our property
                    head = ""
                    layerPreview["previewString"] = head.join(itertools.islice(myfile, 6))
            createOrUpdateEntity(kind="preview", entity=layerPreview)
       
    ifile.close()     

#--------------------[ Main ]-----------------------------
gSYNAPSE.login(gARGS.user, gARGS.password)

#if not checkEmptyRepository():
#    print "Repository is not empty! Aborting..."
#    sys.exit(1)

project = {"name":SAGE_CURATION_PROJECT_NAME, "description":"Umbrella for Sage-curated projects", "creator":"x.schildwachter@sagebase.org"}
storedProject = createOrUpdateEntity(kind="project",
                                     entity=project,
                                     permissions=ROOT_PERMS)
    
eula = {"name":SAGE_CURATION_EULA_NAME, "agreement":DEFAULT_TERMS_OF_USE}
storedEula = createOrUpdateEntity(kind="eula",
                                  entity=eula,
                                  permissions=ROOT_PERMS)
    
loadDatasets(storedProject["id"], storedEula["id"])
    
if(None != gARGS.layersCsv):
    loadLayers()
