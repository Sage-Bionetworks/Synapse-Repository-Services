#!/usr/local/python/2.7.1/bin/python

# To debug this, python -m pdb myscript.py

import os, csv, json, re, string, datetime, pwd, urllib, httplib, ConfigParser, itertools, argparse, codecs, cStringIO

#-------[ Documentation embedded in Command Line Arguments ]----------------
parser = argparse.ArgumentParser(description='Tool to load metadata into a Sage Platform Repository Service.  Note that this script always create instance of a dataset in the repository service (the repository service does not enforce uniqueness of dataset names).  Use the datasetNuker.py script first if you want to start with a clean datastore.')

parser.add_argument('--datasetsCsv', '-d', help='the file path to the CSV file holding dataset metadata, defaults to AllDatasets.csv', default='AllDatasets.csv')

parser.add_argument('--layersCsv', '-l', help='the file path to the CSV file holding layer metadata, defaults to AllDatasetLayerLocations.csv', default='AllDatasetLayerLocations.csv')

parser.add_argument('--serviceEndpoint', '-e', help='the host and optionally port to which to send the metadata', required=True)

parser.add_argument('--servletPrefix', '-p', help='the servlet URL prefix, defaults to /repo/v1', default='/repo/v1')

parser.add_argument('--https', '--secure', '-s', help='whether to do HTTPS instead of HTTP, defaults to False', action='store_true', default=False)

parser.add_argument('--debug', help='whether to output verbose information for debugging purposes, defaults to False', action='store_true', default=False)

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
    'Date Posted': 'releaseDate',
    'Version': 'version',
    }

HEADERS = {
    "Content-type": "application/json",
    "Accept": "application/json",
    }

#-------------------[ Global Variables ]----------------------

# A mapping we build over time of dataset names to layer uris.  In our
# layer CSV file we have the dataset name to which each layer belongs.
gDATASET_NAME_2_LAYER_URI = {}


#-----[ Classes to deal with latin_1 extended chars] ---------
class UTF8Recoder:
    """
    Iterator that reads an encoded stream and reencodes the input to UTF-8
    """
    def __init__(self, f, encoding):
        self.reader = codecs.getreader(encoding)(f)

    def __iter__(self):
        return self

    def next(self):
        return self.reader.next().encode("utf-8")

class UnicodeReader:
    """
    A CSV reader which will iterate over lines in the CSV file "f",
    which is encoded in the given encoding.
    """

    def __init__(self, f, dialect=csv.excel, encoding="utf-8", **kwds):
        f = UTF8Recoder(f, encoding)
        self.reader = csv.reader(f, dialect=dialect, **kwds)

    def next(self):
        row = self.reader.next()
        return [unicode(s, "utf-8") for s in row]

    def __iter__(self):
        return self

class UnicodeWriter:
    """
    A CSV writer which will write rows to CSV file "f",
    which is encoded in the given encoding.
    """

    def __init__(self, f, dialect=csv.excel, encoding="utf-8", **kwds):
        # Redirect output to a queue
        self.queue = cStringIO.StringIO()
        self.writer = csv.writer(self.queue, dialect=dialect, **kwds)
        self.stream = f
        self.encoder = codecs.getincrementalencoder(encoding)()

    def writerow(self, row):
        self.writer.writerow([s.encode("utf-8") for s in row])
        # Fetch UTF-8 output from the queue ...
        data = self.queue.getvalue()
        data = data.decode("utf-8")
        # ... and reencode it into the target encoding
        data = self.encoder.encode(data)
        # write to the target stream
        self.stream.write(data)
        # empty queue
        self.queue.truncate(0)

    def writerows(self, rows):
        for row in rows:
            self.writerow(row)

#--------------------[ createObject ]-----------------------------
def createObject(uri, object):
    if(0 != string.find(uri, args.servletPrefix)):
            uri = args.servletPrefix + uri
    
    conn = httplib.HTTPConnection(args.serviceEndpoint, timeout=30)
    if(args.debug):
        conn.set_debuglevel(10);
    try:
        try:
            conn.request("POST", uri, json.dumps(object), HEADERS)
            resp = conn.getresponse()
            output = resp.read()
            if args.debug:
                print output
            if resp.status == 201:
                object = json.loads(output)
                return object
            else:
                print resp.status, resp.reason
                return None;
        except Exception, err:
            print(err)
    finally:
        conn.close()

#--------------------[ putProperty ]-----------------------------
def putProperty(uri, property):
    if(uri == None):
        return

    conn = httplib.HTTPConnection(args.serviceEndpoint, timeout=30)
    if(args.debug):
        conn.set_debuglevel(2);

    putHeaders = HEADERS
    oldProperty = {}

    try:
        try:
            conn.request("GET", uri, None, HEADERS)
            resp = conn.getresponse()
            output = resp.read()
            if args.debug:
                print output
            if resp.status == 200:
                oldProperty = json.loads(output)
            else:
                print resp.status, resp.reason
        except Exception, err:
            print(err)
            
        putHeaders['ETag'] = oldProperty["etag"]

        # Overwrite our stored fields with our updated fields
        keys = property.keys()
        for key in keys:
            oldProperty[key] = property[key]
        
        try:
            conn.request("PUT", uri, json.dumps(oldProperty), HEADERS)
            resp = conn.getresponse()
            output = resp.read()
            if args.debug:
                print output
            if resp.status == 200:
                object = json.loads(output)
                return object
            else:
                print resp.status, resp.reason
                return None;
        except Exception, err:
            print(err)
    finally:
        conn.close()

#--------------------[ createDataset ]-----------------------------
def createDataset(dataset, annotations):
    print json.dumps(dataset)
    newDataset = createObject("/dataset", dataset)
    # Put our annotations
    print json.dumps(annotations)
    putProperty(newDataset["annotations"], annotations)
    # Stash the layer uri for later use
    gDATASET_NAME_2_LAYER_URI[dataset['name']] = newDataset['layer']
    print
    print
      
#--------------------[ loadDatasets ]-----------------------------
# What follows is code that expects a dataset CSV in a particular format,
# sorry its so brittle and ugly
def loadDatasets():
    # xschildw: Use codecs.open and UnicodeReader class to handle extended chars
    ifile  = open(args.datasetsCsv, "r")
    reader = UnicodeReader(ifile, encoding='latin_1')

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
    stringAnnotations['Tissue/Tumor'] = []
    
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
            stringAnnotations['Tissue/Tumor'] = []
                        
        # Load the row data from the dataset CSV into our datastructure    
        colnum = 0
        for col in row:
            print '%-8s: %s' % (header[colnum], col)
            if(header[colnum] in CSV_TO_PRIMARY_FIELDS):
                if("name" == header[colnum]):
                    cleanName = col.replace("_", " ")
                    dataset[CSV_TO_PRIMARY_FIELDS[header[colnum]]] = cleanName
                else:
                    dataset[CSV_TO_PRIMARY_FIELDS[header[colnum]]] = col
            else:
                # TODO consider reading these into fields
                #             abstract_file_path
                #             citation_file_path
                #             user_agreement_file_path
                #             readme_file_path
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
                            stringAnnotations['Tissue/Tumor'].append(col)
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
    ifile  = open(args.layersCsv, "rU")
    reader = csv.reader(ifile)
    rownum = -1
    for row in reader:
        rownum += 1

        if rownum == 0:
            # Save header row
            header = row
            continue
        
        # xschildw: new format is
        # Dataset Name,type,status,name,Number of samples,Platform,Version,preview,sage,awsebs,awss3
        colnum = 0
        layerUri = gDATASET_NAME_2_LAYER_URI[row[0]]
        layer = {}
        layer["type"] = row[1]
        layer["status"] = row[2]
        layer["name"] = row[3]
        layer["numSamples"] = row[4]
        layer["platform"] = row[5]
        layer["version"] = row[6]
        
        newLayer = createObject(layerUri, layer)
        
        layerLocations = {}
        layerLocations["locations"] = []
        for col in [8,9,10]:
            if(row[col] != ""):
                location = {}
                location["type"] = header[col]
                location["path"] = row[col];
                layerLocations["locations"].append(location)
        putProperty(newLayer["locations"][0], layerLocations);
        
        layerPreview = {}
        
        if(row[7] != ""):
            with open(row[7]) as myfile:
                # Slurp in the first six lines of the file and store
                # it in our property
                head = ""
                layerPreview["preview"] = head.join(itertools.islice(myfile,6))
                putProperty(newLayer["preview"], layerPreview)
    ifile.close()     

#--------------------[ Main ]-----------------------------

args = parser.parse_args()

loadDatasets()

if(None != args.layersCsv):
    loadLayers()

