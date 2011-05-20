#!/usr/bin/env python2.7

# To debug this, python -m pdb myscript.py

import os, csv, json, re, string, datetime, pwd, urllib, httplib, ConfigParser, itertools, argparse, codecs, cStringIO, synapse.client, synapse.utils

#-------[ Documentation embedded in Command Line Arguments ]----------------
parser = synapse.utils.createBasicArgParser('Tool to load metadata into a Sage Platform Repository Service.  Note that this script always create instance of a dataset in the repository service (the repository service does not enforce uniqueness of dataset names).  Use the datasetNuker.py script first if you want to start with a clean datastore.')

parser.add_argument('--datasetsCsv', '-d', help='the file path to the CSV file holding dataset metadata, defaults to AllDatasets.csv', default='AllDatasets.csv')

parser.add_argument('--layersCsv', '-l', help='the file path to the CSV file holding layer metadata, defaults to AllDatasetLayerLocations.csv', default='AllDatasetLayerLocations.csv')

parser.add_argument('--md5sumCsv', '-m', help='the file path to the CSV file holding the md5sums for files, defaults to ../platform.md5sums.csv', default='../platform.md5sums.csv')

synapse.client.addArguments(parser)

#-------------------[ Constants ]----------------------

NOW = datetime.datetime.today()

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

HEADERS = {
    "Content-type": "application/json",
    "Accept": "application/json",
    }

#-------------------[ Global Variables ]----------------------

# Command line arguments
gARGS = {}
gARGS = parser.parse_args()
gSYNAPSE = synapse.client.factory(gARGS)

# A mapping we build over time of dataset names to layer uris.  In our
# layer CSV file we have the dataset name to which each layer belongs.
gDATASET_NAME_2_LAYER_URI = {}

# A mapping of files to their md5sums
gFILE_PATH_2_MD5SUM = {}

#--------------------[ createDataset ]-----------------------------
def createDataset(dataset, annotations):
    newDataset = gSYNAPSE.createEntity("/dataset", dataset)
    # Put our annotations
    gSYNAPSE.updateEntity(newDataset["annotations"], annotations)
    # Stash the layer uri for later use
    gDATASET_NAME_2_LAYER_URI[dataset['name']] = newDataset['layer']
    print 'Created Dataset %s\n\n' % (dataset['name'])
      
#--------------------[ loadMd5sums ]-----------------------------
def loadMd5sums():
    ifile  = open(gARGS.md5sumCsv, "rU")
    for line in ifile:
        row = string.split(line.rstrip())
        md5sum = row[0]
        # strip off any leading forward slashes
        filePath = string.lstrip(row[1], "/")
        gFILE_PATH_2_MD5SUM[filePath] = md5sum
        
#--------------------[ loadDatasets ]-----------------------------
# What follows is code that expects a dataset CSV in a particular format,
# sorry its so brittle and ugly
def loadDatasets():
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
            createDataset(dataset, annotations)
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
            else:
                if( re.search('date', string.lower(header[colnum])) ):
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
    ifile.close()     

    # Send the last one, create our dataset
    createDataset(dataset, annotations)

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
        layerUri = gDATASET_NAME_2_LAYER_URI[row[0]]
        layer = {}
        layer["type"] = row[1]
        layer["status"] = row[2]
        layer["name"] = row[3]
        layer["numSamples"] = row[4]
        layer["platform"] = row[5]
        layer["version"] = row[6]
        layer["qcBy"] = row[11]
        
        newLayer = gSYNAPSE.createEntity(layerUri, layer)
        print 'Created layer %s for %s\n\n' % (layer["name"], row[0])
        
        layerLocations = {}
        layerLocations["locations"] = []
        for col in [8,9,10]:
            if(row[col] != ""):
                # trim whitespace off both sides
                path = row[col].strip()
                location = {}
                location["type"] = header[col]
                location["path"] = path
                if(path in gFILE_PATH_2_MD5SUM):
                    location["md5sum"] = gFILE_PATH_2_MD5SUM[path]
                layerLocations["locations"].append(location)
        gSYNAPSE.updateEntity(newLayer["locations"][0], layerLocations);
        
        layerPreview = {}
       
        if(row[7] != ""):
            with open(row[7]) as myfile:
                # Slurp in the first six lines of the file and store
                # it in our property
                head = ""
                layerPreview["preview"] = head.join(itertools.islice(myfile,6))
                gSYNAPSE.updateEntity(newLayer["preview"], layerPreview)
    ifile.close()     

#--------------------[ Main ]-----------------------------

loadMd5sums()
loadDatasets()

if(None != gARGS.layersCsv):
    loadLayers()

