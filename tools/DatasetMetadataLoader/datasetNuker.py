#!/usr/bin/env python2.7

# To debug this, python -m pdb myscript.py

import os, json, re, string, datetime, pwd, urllib, httplib, ConfigParser, argparse, synapse.utils, synapse.client

#-------[ Documentation embedded in Command Line Arguments ]----------------
parser = synapse.utils.createBasicArgParser('Tool to nuke all datasets currently stored in the repository service.  You might run this prior to datasetCsvLoader.py.')

synapse.client.addArguments(parser)

#--------------------[ Main ]-----------------------------

args = parser.parse_args()
synapse = synapse.client.factory(args)
allDatasets = synapse.getEntity("/dataset?limit=500");
for dataset in allDatasets["results"]:
    print "About to nuke: " + dataset["uri"]
    synapse.deleteEntity(dataset["uri"])
    
