#!/usr/bin/env Rscript

source('./src/main/resources/synapseWorkflow.R')

## #----- Load the Synpase R client
## library(synapseClient)
## 
## #----- Log into Synapse
## if(!is.null(getAuthEndpointArg())) {
##   synapseAuthServiceEndpoint(getAuthEndpointArg())
## }
## if(!is.null(getRepoEndpointArg())) {
##   synapseRepoServiceEndpoint(getRepoEndpointArg())
## }
## synapseLogin(getUsernameArg(), getPasswordArg())
## 
## #----- Unpack the rest of our command line parameters
## inputLayerId <- getInputLayerIdArg()

inputDatasetId <- getInputDatasetIdArg()


finishWorkflowTask(output=paste("Hello world! datasetid=",inputDatasetId))

