#!/usr/bin/env python2.7

# To debug this, python -m pdb myscript.py

import os, json, re, string, datetime, pwd, urllib, httplib, ConfigParser, argparse

#-------[ Documentation embedded in Command Line Arguments ]----------------
parser = argparse.ArgumentParser(description='Tool to nuke all datasets currently stored in the repository service.  You might run this prior to datasetCsvLoader.py.')

parser.add_argument('--serviceEndpoint', '-e', help='the host and optionally port to which to send the metadata', required=True)

parser.add_argument('--servletPrefix', '-p', help='the servlet URL prefix, defaults to /repo/v1', default='/repo/v1')

parser.add_argument('--https', '--secure', '-s', help='whether to do HTTPS instead of HTTP, defaults to False', action='store_true', default=False)

parser.add_argument('--debug', help='whether to output verbose information for debugging purposes, defaults to False', action='store_true', default=False)

#-------------------[ Constants ]----------------------
HEADERS = {
    "Accept": "application/json",
    }

#--------------------[ getAllDatasets ]-----------------------------
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

#--------------------[ nukeObject ]-----------------------------
def nukeObject(uri):
    if(0 != string.find(uri, args.servletPrefix)):
            uri = args.servletPrefix + uri

    conn = httplib.HTTPConnection(args.serviceEndpoint, timeout=30)
    if(args.debug):
        conn.set_debuglevel(10);
    try:
        conn.request("DELETE", uri, None, HEADERS)
        resp = conn.getresponse()
        output = resp.read()
        if args.debug:
            print output
        if resp.status != 204:
            print resp.status, resp.reason
        return None;
    except Exception, err:
        print(err)
    conn.close()

#--------------------[ Main ]-----------------------------

args = parser.parse_args()

allDatasets = getAllDatasets();
for dataset in allDatasets:
    print "About to nuke: " + dataset["uri"]
    nukeObject(dataset["uri"])
    
