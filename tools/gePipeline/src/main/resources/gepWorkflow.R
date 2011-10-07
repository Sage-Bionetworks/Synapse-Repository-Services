#!/usr/bin/env Rscript

source('./src/main/resources/synapseWorkflow.R')

gseId <- getInputDatasetIdArg()
geoTimestamp  <- getLastUpdateDateArg()
userName <- getUsernameArg()
secretKey <- getSecretKeyArg()
authEndpoint <- getAuthEndpointArg()
repoEndpoint <- getRepoEndpointArg()
projectId <- getProjectId()

finishWorkflowTask(output=paste("datasetid:",gseId, 
				"Last Updated:", geoTimestamp, 
				"userName:", userName,
				"secretKey:", secretKey,
				"authEndpoint:", authEndpoint,
				"repoEndpoint:", repoEndpoint
				))

