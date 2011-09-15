#!/usr/bin/env python2.7

# To debug this, python -m pdb myscript.py

import argparse, re, os, sys, synapse.client, synapse.utils

#-------[ Documentation embedded in Command Line Arguments ]----------------
parser = synapse.utils.createBasicArgParser('Tool to load metadata into a Sage Platform Repository Service.  Note that this script always create instance of a dataset in the repository service (the repository service does not enforce uniqueness of dataset names).  Use the datasetNuker.py script first if you want to start with a clean datastore.')

synapse.client.addArguments(parser)

parser.add_argument('--layersCsv', '-l', help='BATCH MODE: the file path to the CSV file holding layer metadata, defaults to AllDatasetLayerLocations.csv', default='AllDatasetLayerLocations.csv')

parser.add_argument('--layerId', '-i', help='SINGLE UPDATE: the Synapse id of the layer to updateuse fake data when we would normally read something from the actual Sage Bionetworks datasets, defaults to False')

parser.add_argument('--descriptionFile', '-f', help='SINGLE UPDATE: the path to the description file to use')

# Command line arguments
gARGS = {}
gARGS = parser.parse_args()
gSYNAPSE = synapse.client.factory(gARGS)

#-------------------[ Constants ]----------------------
DESCRIPTION_FILE = 'description.txt'
DESCRIPTION_ANNOTATION_KEY_PREFIX = 'colDesc_'
UNITS_ANNOTATION_KEY_PREFIX = 'colUnits_'

#--------------------[ updateTraitDescription ]-----------------------------
def updateTraitDescriptionById(layerId, descriptionFile):
    layer = gSYNAPSE.getRepoEntity(uri='/layer/' + layerId)
    updateTraitDescription(layer=layer, descriptionFile=descriptionFile)

#--------------------[ updateTraitDescription ]-----------------------------
def updateTraitDescription(layer, descriptionFile):

    if(not os.access(descriptionFile, os.R_OK)):
        raise Exception("\nCannot update layerId %s, file %s is not accessible"
                        % (layer['id'], descriptionFile))
    print "\nAbout to update layerId %s using descriptions from %s" % (layer['id'],
                                                                     descriptionFile)

    try:
        ifile  = open(descriptionFile, "r")
    except IOError as e:
        raise Exception("unable to open file(" + descriptionFile + "): " + e)
    reader = synapse.utils.UnicodeReader(ifile, encoding='latin_1', delimiter='\t')
    annotations = {}
    stringAnnotations = {}
    doubleAnnotations = {}
    longAnnotations = {}
    dateAnnotations = {}
    annotations['stringAnnotations'] = stringAnnotations
    annotations['doubleAnnotations'] = doubleAnnotations
    annotations['longAnnotations'] = longAnnotations
    annotations['dateAnnotations'] = dateAnnotations
    rownum = -1
    for row in reader:
        rownum += 1
        
        if rownum == 0:
            # Save header row
            header = row
            if(('phenotype_id' != header[0])
               or ('description' != header[4])
               or ('units' != header[5])):
                raise Exception('malformed description file: ' + descriptionFile)
            continue

        descKey = DESCRIPTION_ANNOTATION_KEY_PREFIX + row[0]
        unitKey = UNITS_ANNOTATION_KEY_PREFIX + row[0]

        # replace all non (alphanumeric characters || underscore) with
        # an underscore
        descKey = re.sub(pattern='[\W]', repl='_', string=descKey, count=0)
        unitKey = re.sub(pattern='[\W]', repl='_', string=unitKey, count=0)

        stringAnnotations[descKey] = [row[4]]
        stringAnnotations[unitKey] = [row[5]]

    gSYNAPSE.updateRepoEntity(layer["annotations"], annotations)
    print "Updated layerId %s using descriptions from %s" % (layer['id'],
                                                             descriptionFile)
    
#--------------------[ updateTraitDescriptions ]-----------------------------
def updateTraitDescriptions():
    ifile  = open(gARGS.layersCsv, "r")
    reader = synapse.utils.UnicodeReader(ifile, encoding='latin_1')
    rownum = -1
    for row in reader:
        rownum += 1

        if rownum == 0:
            # Save header row
            header = row
            continue

        
        if("C" != row[1] or "" == row[7]):
            # skip non-clinical layers or clinical layers without a preview
            continue
        
        datasetName = row[0]
        layerName = row[3]
        previewFile = row[7]

        dataset = gSYNAPSE.getRepoEntityByProperty(kind="dataset",
                                                   propertyName="name",
                                                   propertyValue=datasetName)
        layer = gSYNAPSE.getRepoEntityByProperty(kind="layer",
                                                 propertyName="name",
                                                 propertyValue=layerName,
                                                 parentId=dataset['id'])

        descriptionFile = os.path.dirname(previewFile) + '/' + DESCRIPTION_FILE
        try: 
            updateTraitDescription(layer=layer,
                                   descriptionFile=descriptionFile)
        except Exception as e:
            # swallow error and keep going, some datasets have a
            # preview but no description.txt file
            print e
        
    ifile.close()     

#--------------------[ Main ]-----------------------------
gSYNAPSE.login(gARGS.user, gARGS.password)

if(None != gARGS.layerId):
    updateTraitDescriptionById(layerId=gARGS.layerId,
                               descriptionFile=gARGS.descriptionFile)
else:
    updateTraitDescriptions()
