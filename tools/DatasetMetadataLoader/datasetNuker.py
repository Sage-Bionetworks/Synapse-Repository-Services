#!/usr/bin/env python2.7

# To debug this, python -m pdb myscript.py

import os, json, re, string, datetime, pwd, urllib, httplib, ConfigParser, argparse, synapse.utils, synapse.client

#-------[ Documentation embedded in Command Line Arguments ]----------------
parser = synapse.utils.createBasicArgParser('Tool to nuke all datasets currently stored in the repository service.  You might run this prior to datasetCsvLoader.py.')

synapse.client.addArguments(parser)

#-------------------[ Constants ]----------------------
HEADERS = {
    "Accept": "application/json",
    }

#--------------------[ getAllDatasets ]-----------------------------
# TODO put an improved version of this in synapse.client
def getAllDatasets():
    conn = httplib.HTTPConnection(args.serviceEndpoint, timeout=30)
    if(args.debug):
        conn.set_debuglevel(10);
    try:
        conn.request("GET", args.servletPrefix + "/dataset?limit=500", None, HEADERS)
        resp = conn.getresponse()
        output = resp.read()
        if args.debug:
            print output
        if resp.status == 200:
            results = json.loads(output)
            return results["results"]
        else:
            print resp.status, resp.reason
            return None;
    except Exception, err:
        print(err)
    conn.close()

#--------------------[ Main ]-----------------------------

args = parser.parse_args()
synapse = synapse.client.factory(args)
allDatasets = getAllDatasets();
for dataset in allDatasets:
    print "About to nuke: " + dataset["uri"]
    synapse.deleteEntity(dataset["uri"])
    
