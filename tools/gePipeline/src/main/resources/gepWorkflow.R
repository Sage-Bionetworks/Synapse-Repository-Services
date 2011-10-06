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

gseId <- getInputDatasetIdArg()
geoTimestamp  <- getLastUpdateDateArg()
userName <- getUsernameArg()
secretKey <- getSecretKeyArg()
authEndpoint <- getAuthEndpointArg()
repoEndpoint <- getRepoEndpointArg()

finishWorkflowTask(output=paste("datasetid:",gseId, 
				"Last Updated:", geoTimestamp, 
				"userName:", userName,
				"secretKey:", secretKey,
				"authEndpoint:", authEndpoint,
				"repoEndpoint:", repoEndpoint
				))

