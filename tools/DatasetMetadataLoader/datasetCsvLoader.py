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
    "AUTHENTICATED_USERS":["READ"],
    "PUBLIC":["READ"]
    }

LOCATION_PERMS = {
    "Sage Curators":["READ","CHANGE_PERMISSIONS","UPDATE","CREATE"],
    "AUTHENTICATED_USERS":["READ"],
    "PUBLIC":None  # no permissions
    }

DEFAULT_TERMS_OF_USE = """Copyright 2011 Sage Bionetworks

Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance with the License. You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions andlimitations under the License.

1. Definitions.

"License" shall mean the terms and conditions for use, reproduction, and distribution as defined by Sections 1 through 9 of this document.

"Licensor" shall mean the copyright owner or entity authorized by the copyright owner that is granting the License.

"Legal Entity" shall mean the union of the acting entity and all other entities that control, are controlled by, or are under common control with that entity. For the purposes of this definition, "control" means (i) the power, direct or indirect, to cause the direction or management of such entity, whether by contract or otherwise, or (ii) ownership of fifty percent (50%) or more of the outstanding shares, or (iii) beneficial ownership of such entity.

"You" (or "Your") shall mean an individual or Legal Entity exercising permissions granted by this License.

"Source" form shall mean the preferred form for making modifications, including but not limited to software source code, documentation source, and configuration files.

"Object" form shall mean any form resulting from mechanical transformation or translation of a Source form, including but not limited to compiled object code, generated documentation, and conversions to other media types.

"Work" shall mean the work of authorship, whether in Source or Object form, made available under the License, as indicated by a copyright notice that is included in or attached to the work (an example is provided in the Appendix below).

"Derivative Works" shall mean any work, whether in Source or Object form, that is based on (or derived from) the Work and for which the editorial revisions, annotations, elaborations, or other modifications represent, as a whole, an original work of authorship. For the purposes of this License, Derivative Works shall not include works that remain separable from, or merely link (or bind by name) to the interfaces of, the Work and Derivative Works thereof.

"Contribution" shall mean any work of authorship, including the original version of the Work and any modifications or additions to that Work or Derivative Works thereof, that is intentionally submitted to Licensor for inclusion in the Work by the copyright owner or by an individual or Legal Entity authorized to submit on behalf of the copyright owner. For the purposes of this definition, "submitted" means any form of electronic, verbal, or written communication sent to the Licensor or its representatives, including but not limited to communication on electronic mailing lists, source code control systems, and issue tracking systems that are managed by, or on behalf of, the Licensor for the purpose of discussing and improving the Work, but excluding communication that is conspicuously marked or otherwise designated in writing by the copyright owner as "Not a Contribution."

"Contributor" shall mean Licensor and any individual or Legal Entity on behalf of whom a Contribution has been received by Licensor and subsequently incorporated within the Work.

2. Grant of Copyright License. Subject to the terms and conditions of this License, each Contributor hereby grants to You a perpetual, worldwide, non-exclusive, no-charge, royalty-free, irrevocable copyright license to reproduce, prepare Derivative Works of, publicly display, publicly perform, sublicense, and distribute the Work and such Derivative Works in Source or Object form.

3. Grant of Patent License. Subject to the terms and conditions of this License, each Contributor hereby grants to You a perpetual, worldwide, non-exclusive, no-charge, royalty-free, irrevocable (except as stated in this section) patent license to make, have made, use, offer to sell, sell, import, and otherwise transfer the Work, where such license applies only to those patent claims licensable by such Contributor that are necessarily infringed by their Contribution(s) alone or by combination of their Contribution(s) with the Work to which such Contribution(s) was submitted. If You institute patent litigation against any entity (including a cross-claim or counterclaim in a lawsuit) alleging that the Work or a Contribution incorporated within the Work constitutes direct or contributory patent infringement, then any patent licenses granted to You under this License for that Work shall terminate as of the date such litigation is filed.

4. Redistribution. You may reproduce and distribute copies of the Work or Derivative Works thereof in any medium, with or without modifications, and in Source or Object form, provided that You meet the following conditions:

You must give any other recipients of the Work or Derivative Works a copy of this License; and

You must cause any modified files to carry prominent notices stating that You changed the files; and

You must retain, in the Source form of any Derivative Works that You distribute, all copyright, patent, trademark, and attribution notices from the Source form of the Work, excluding those notices that do not pertain to any part of the Derivative Works; and

If the Work includes a "NOTICE" text file as part of its distribution, then any Derivative Works that You distribute must include a readable copy of the attribution notices contained within such NOTICE file, excluding those notices that do not pertain to any part of the Derivative Works, in at least one of the following places: within a NOTICE text file distributed as part of the Derivative Works; within the Source form or documentation, if provided along with the Derivative Works; or, within a display generated by the Derivative Works, if and wherever such third-party notices normally appear. The contents of the NOTICE file are for informational purposes only and do not modify the License. You may add Your own attribution notices within Derivative Works that You distribute, alongside or as an addendum to the NOTICE text from the Work, provided that such additional attribution notices cannot be construed as modifying the License. You may add Your own copyright statement to Your modifications and may provide additional or different license terms and conditions for use, reproduction, or distribution of Your modifications, or for any such Derivative Works as a whole, provided Your use, reproduction, and distribution of the Work otherwise complies with the conditions stated in this License.

5. Submission of Contributions. Unless You explicitly state otherwise, any Contribution intentionally submitted for inclusion in the Work by You to the Licensor shall be under the terms and conditions of this License, without any additional terms or conditions. Notwithstanding the above, nothing herein shall supersede or modify the terms of any separate license agreement you may have executed with Licensor regarding such Contributions.

6. Trademarks. This License does not grant permission to use the trade names, trademarks, service marks, or product names of the Licensor, except as required for reasonable and customary use in describing the origin of the Work and reproducing the content of the NOTICE file.

7. Disclaimer of Warranty. Unless required by applicable law or agreed to in writing, Licensor provides the Work (and each Contributor provides its Contributions) on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied, including, without limitation, any warranties or conditions of TITLE, NON-INFRINGEMENT, MERCHANTABILITY, or FITNESS FOR A PARTICULAR PURPOSE. You are solely responsible for determining the appropriateness of using or redistributing the Work and assume any risks associated with Your exercise of permissions under this License.

8. Limitation of Liability. In no event and under no legal theory, whether in tort (including negligence), contract, or otherwise, unless required by applicable law (such as deliberate and grossly negligent acts) or agreed to in writing, shall any Contributor be liable to You for damages, including any direct, indirect, special, incidental, or consequential damages of any character arising as a result of this License or out of the use or inability to use the Work (including but not limited to damages for loss of goodwill, work stoppage, computer failure or malfunction, or any and all other commercial damages or losses), even if such Contributor has been advised of the possibility of such damages.

9. Accepting Warranty or Additional Liability. While redistributing the Work or Derivative Works thereof, You may choose to offer, and charge a fee for, acceptance of support, warranty, indemnity, or other liability obligations and/or rights consistent with this License. However, in accepting such obligations, You may act only on Your own behalf and on Your sole responsibility, not on behalf of any other Contributor, and only if You agree to indemnify, defend, and hold each Contributor harmless for any liability incurred by, or claims asserted against, such Contributor by reason of your accepting any such warranty or additional liability.
"""

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
    #chkList = ["/project", "/dataset", "/layer", "/preview"]
    #for c in chkList:
    #    l = gSYNAPSE.getRepoEntity(c)
    #    if 0 == len(l):
    #        return False
    return True

def setEntityPermissions(permissionList, storedEntity):
    """
    Helper function to merge some additional permissions into the
    existing access list for an entity
    """
    principals = gSYNAPSE.getPrincipals()
    currentAcl = gSYNAPSE.getRepoEntity(storedEntity["accessControlList"])
    print("Old acl %s %s" % (currentAcl['resourceAccess'], storedEntity["uri"]))

    accessList = []
    # Copy over existing permissions, if not removed
    for permission in currentAcl['resourceAccess']:
        if(permission['groupName'] in permissionList):
               if(None == permissionList[permission['groupName']]):
                   # remove the existing permission
                   continue
               if(permission['groupName'] in principals):
                   # update the existing permission
                   permission['accessType'] = permissionList[permission['groupName']]
        accessList.append(permission)

    # Add any new permissions
    for p in principals:
        if p["name"] in permissionList:
            groupAlreadyInAccessList = False
            for permission in currentAcl['resourceAccess']:
                if(permission["groupName"] == p['name']):
                    groupAlreadyInAccessList = True
            if(not groupAlreadyInAccessList):
                # append the permissions
                accessList.append({"groupName":p["name"], "accessType":permissionList[p["name"]]})

    acl = {"resourceAccess":accessList, "id":storedEntity["id"]}

    print("New acl %s %s" % (accessList, storedEntity["uri"]))
    
    if(storedEntity["id"] != currentAcl["id"]):
        gSYNAPSE.createRepoEntity(storedEntity["accessControlList"], acl)
    else:
        gSYNAPSE.updateRepoEntity(storedEntity["accessControlList"], acl)

def createOrUpdateEntity(kind, entity, permissions=None):
    """
    Helper function to query to determine whether the entity exists
    and if so updated instead of create the entity.  Note that
    different entities need different queries to find the unique
    instance.

    Note that permissions defaults to None meaning that this entity
    should just inherit the permissions of its parent.    
    """
    if("preview" == kind):
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
        print 'Created %s %s\n\n' % (kind, message)
    else:
        storedEntity = gSYNAPSE.updateRepoEntity(storedEntity["uri"], entity)
        print 'Updated %s %s\n\n' % (kind, message)

#    if(None != permissions):
#        accessList = setEntityPermissions(permissions, storedEntity)
#        print 'Updated acl %s %s\n\n' % (kind, message)
    return storedEntity

def createOrUpdateDataset(dataset, annotations):
    """
    Helper function to create or update a dataset, its annotations,
    and its location as appropriate.
    """
    storedDataset = createOrUpdateEntity(kind="dataset", entity=dataset)
        
    # Put our annotations
    gSYNAPSE.updateRepoEntity(storedDataset["annotations"], annotations)

    # Stash the dataset id for later use
    gDATASET_NAME_2_ID[dataset['name']] = storedDataset['id']
              
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
                                  annotations=annotations)
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
                    locationData = {}
                    locationData["type"] = "external"
                    locationData["path"] = path
                    dataset["md5"] = "43809069fd7d431cd17aec5fac064b95"
                    dataset[locations] = []
                    dataset[locations][0] = locationData
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
                          annotations=annotations)

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
        
        # Ignore column 8 (sage loc) and 9 (awsebs loc) for now
        for col in [10]:
            if(row[col] != ""):
                # trim whitespace off both sides
                path = row[col].strip()
                locationData = {}
                locationData["type"] = "external"
                locationData["path"] = path
                layer["md5"] = "43809069fd7d431cd17aec5fac064b95"
                layer[locations] = []
                layer[locations][0] = locationData
        
        newLayer = createOrUpdateEntity(kind="layer", entity=layer)
        if newLayer == None:
            raise Exception("ENTITY_CREATION_ERROR")
        
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
storedEula = {}
try:
    # Only admins can update eulas
    storedEula = createOrUpdateEntity(kind="eula",
                                      entity=eula)
except Exception, err:
    storedEula = gSYNAPSE.getRepoEntityByName(kind="eula", name=SAGE_CURATION_EULA_NAME)
    
loadDatasets(storedProject["id"], storedEula["id"])
    
if(None != gARGS.layersCsv):
    loadLayers()
