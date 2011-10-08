#!/usr/bin/env Rscript

###############################################################################
# Run the metaGeo QC and load the results to Synapse
# 
# Author: Matt Furia
###############################################################################

## constants
kErrorStatusCode <- 1L
kOkStatusCode <- 0L

## load synapse client
library(synapseClient)

## load the workflow utilities
source('./src/main/resources/synapseWorkflow.R')

## get config args
gseId <- getInputDatasetIdArg()
geoTimestamp  <- getLastUpdateDateArg()
userName <- getUsernameArg()
secretKey <- getSecretKeyArg()
authEndpoint <- getAuthEndpointArg()
repoEndpoint <- getRepoEndpointArg()
projectId <- getProjectId()

if(is.null(gseId) 
		|| is.null(secretKey) 
		|| is.null(userName) 
		|| is.null(geoTimestamp)
		|| is.null(projectId)
		|| is.null(authEndpoint)
		|| is.null(repoEndpoint)
){
	cat("gseId: ", gseId, "\ngeoTimestamp: ", geoTimestamp, "\nuserName: ", userName, "\nsecretKey: ", secretKey, "\nauthEndpoint:", authEndpoint, "\nrepoEndpoint: ", repoEndpoint, "\nprojectId: ", projectId, "\n")
	stop("not all required arguments were provided")
}

cat("gseId: ", gseId, "\ngeoTimestamp: ", geoTimestamp, "\nuserName: ", userName, "\nsecretKey: ", secretKey, "\nauthEndpoint:", authEndpoint, "\nrepoEndpoint: ", repoEndpoint, "\nprojectId: ", projectId, "\n")


## set the service endpoints
synapseAuthServiceEndpoint(authEndpoint)
synapseRepoServiceEndpoint(repoEndpoint)

## set up the hmac credentials
cat("1\n\n")
synapseClient:::userName(userName)
cat("2\n\n")
hmacSecretKey(secretKey)

cat("3\n\n")

##########################################
## get the R Code dataset id
##########################################
result <- synapseQuery(sprintf('select * from dataset where dataset.name == "%s" and dataset.parentId == "%s"', "GEO R Code Layers", projectId))
if(is.null(result) || nrow(result) != 1L){
	msg <- sprintf("could not find R code dataset in project %s", projectId)
	finishWorkflowTask(list(status=kErrorStatusCode,msg=msg))
	stop(msg)
}
datasetId <- result$dataset.id




##########################################
## get the entity ids for the code modules
##########################################
scriptName <- "AddGeoLocationToLayerRScript"
result <- synapseQuery(sprintf('select * from layer where layer.name == "%s" and layer.parentId == "%s"', scriptName, datasetId))
if(nrow(result) != 1L){
	msg <- sprintf("could not find R code entity %s in dataset %s", scriptName, datasetId)
	finishWorkflowTask(list(status=kErrorStatusCode,msg=msg))
	stop(msg)
}
kAddLocationCodeEntityId <- result$layer.id

scriptName <- "CreateGeoExpressionLayerRScript"
result <- synapseQuery(sprintf('select * from layer where layer.name == "%s" and layer.parentId == "%s"', scriptName, datasetId))
if(nrow(result) != 1L){
	msg <- sprintf("could not find R code entity %s in dataset %s", scriptName, datasetId)
	finishWorkflowTask(list(status=kErrorStatusCode,msg=msg))
	stop(msg)
}
kRegisterCodeEntityId <- result$layer.id

scriptName <- "run Metageo QC"
result <- synapseQuery(sprintf('select * from layer where layer.name == "%s" and layer.parentId == "%s"', scriptName, datasetId))
if(nrow(result) != 1L){
	msg <- sprintf("could not find R code entity %s in dataset %s", scriptName, datasetId)
	finishWorkflowTask(list(status=kErrorStatusCode,msg=msg))
	stop(msg)
}
kRunMetaGeoCodeEntityId <- result$layer.id

##########################################
## finished getting code module ids
##########################################

## get registration code entity and register geoId
ans <- tryCatch({
			regCodeEntity <- loadEntity(kRegisterCodeEntityId)
			regCodeEntity$objects$createGeoExpressionLayer(gseId, geoTimestamp, projectId)
		},
		error = function(e){
			msg <- sprintf("Failed to add Layer entity: %s", e)
			finishWorkflowTask(list(status=kErrorStatusCode,errormsg=msg))
			stop(e)
		}
)
dsId <- ans$datasetId
if(!ans$update){
	msg <- sprintf("Dataset %s has not changed since last update.", dsId)
	finishWorkflowTask(list(status=kOkStatusCode, msg=msg, datasetId=dsId))
	stop(msg)
}


## get add location code entity and add location for the geo id
ans <- tryCatch({
			addLocCodeEntity <- loadEntity(kAddLocationCodeEntityId)
			addLocCodeEntity$objects$addGeoLocationToLayer(ans$layerId)
		},
		error = function(e){
			msg <- sprintf("Failed to add Location to data layer: %s", e)
			finishWorkflowTask(list(status=kErrorStatusCode,msg=msg))
			stop(e)
		}
)

## run the metaGeo workflow
ans <- tryCatch({
			runMetaGeoEntity <- loadEntity(kRunMetaGeoCodeEntityId)
			runMetaGeoEntity$objects$run(ans$layerId)
		},
		error = function(e){
			msg <- sprintf("Failed to run metaGeo workflow: %s", e)
			finishWorkflowTask(list(status=kErrorStatusCode,msg=msg))
			stop(e)
		}
)

## call the finish workflow step code
finishWorkflowTask(
		output=list(
				status=kOkStatusCode, 
				msg=sprintf("Successfully added GEO study %s to Synapse.", gseId), 
				datasetId=dsId, 
				qcdExprLayerId=ans$exprLayers, 
				metadataLayerId=ans$metadataLayers
		)
)
