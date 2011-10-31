#!/usr/bin/env Rscript

###############################################################################
# Run the metaGeo QC and load the results to Synapse
# 
# Author: Matt Furia
###############################################################################

starttime<-proc.time()

## constants
kErrorStatusCode <- 1L
kOkStatusCode <- 0L

## load synapse client
library(synapseClient)

synapseCacheDir("/mnt/ebs/synapseCacheDir")
Sys.setenv(TMPDIR="/mnt/ebs/r_tmp")


## load the workflow utilities
source('./src/main/resources/synapseWorkflow.R')

## get config args
userName <- getUsernameArg()
secretKey <- getSecretKeyArg()
authEndpoint <- getAuthEndpointArg()
repoEndpoint <- getRepoEndpointArg()

urlEncodedInputData <- getInputDataArg()
# to avoid problems with spaces, quotes, etc. we just URLEncode the input data
# thus we decode it here
inputData <- URLdecode(urlEncodedInputData)
inputDataMap<-RJSONIO::fromJSON(inputData, simplify=F)


# divides attributes into 'properties' and 'annotations'
splitDatasetAttributes<-function(a) {
	dataSetPropertyLabels<-c("name", "description", "status", "creator", "parentId")
	properties<-list()
	annotations<-list()
	for (i in 1:length(a)) {
		fieldName<-names(a[i])
		if (any(dataSetPropertyLabels==fieldName)) {
			properties[fieldName]<-a[i]
		} else {
			annotations[fieldName]<-a[i]
		}
	}
	list(properties=properties, annotations=annotations)
}


attributes<-splitDatasetAttributes(inputDataMap)

if(!all(c('name', 'parentId', 'lastUpdate') %in% names(inputDataMap))){
	msg <- paste("gseId: ", inputDataMap[["name"]], "\ngeoTimestamp: ", inputDataMap[["lastUpdate"]], "\nuserName: ", userName, "\nsecretKey: ", secretKey, "\nauthEndpoint:", authEndpoint, "\nrepoEndpoint: ", repoEndpoint, "\nprojectId: ", inputDataMap[["parentId"]], "\n")
	msg <- sprintf("not all required properties were provided: %s", msg)
	finishWorkflowTask(list(status=kErrorStatusCode,msg=msg))
	stop(msg)
}

if(is.null(secretKey) 
		|| is.null(userName) 
		|| is.null(authEndpoint)
		|| is.null(repoEndpoint)
		){
	msg <- paste("gseId: ", inputDataMap[["name"]], "\ngeoTimestamp: ", inputDataMap[["lastUpdate"]], "\nuserName: ", userName, "\nsecretKey: ", secretKey, "\nauthEndpoint:", authEndpoint, "\nrepoEndpoint: ", repoEndpoint, "\nprojectId: ", inputDataMap[["parentId"]], "\n")
	msg <- sprintf("not all required arguments were provided: %s", msg)
	finishWorkflowTask(list(status=kErrorStatusCode,msg=msg))
	stop(msg)
}

msg <- paste("gseId: ", inputDataMap[["name"]], "\ngeoTimestamp: ", inputDataMap[["lastUpdate"]], "\nuserName: ", userName, "\nsecretKey: ", secretKey, "\nauthEndpoint:", authEndpoint, "\nrepoEndpoint: ", repoEndpoint, "\nprojectId: ", inputDataMap[["parentId"]], "\n")
cat(msg)

## set the service endpoints
synapseAuthServiceEndpoint(authEndpoint)
synapseRepoServiceEndpoint(repoEndpoint)

## set up the hmac credentials
synapseClient:::userName(userName)
hmacSecretKey(secretKey)

## create the geo dataset
ans <- tryCatch({
			createDataset(attributes$properties, attributes$annotations)
		},
		error = function(e){
			msg <- sprintf("Failed to create Dataset: %s", e)
			finishWorkflowTask(list(status=kErrorStatusCode,errormsg=msg))
			stop(e)
		}
)
dsId <- ans$datasetId
msg <- "Starting"
setWorkFlowStatusAnnotation(dsId, kOkStatusCode, msg)

if(!ans$update){
	msg <- sprintf("Dataset %s has not changed since last update.", dsId)
	finishWorkflowTask(list(status=kOkStatusCode, msg=msg, datasetId=dsId))
	setWorkFlowStatusAnnotation(dsId, kOkStatusCode, msg)
}else{
	
	##########################################
	## get the R Code dataset id
	##########################################
	codeProjectId <- 17962 ### TEMPORARY FIX
	result <- synapseQuery(sprintf('select * from dataset where dataset.name == "%s" and dataset.parentId == "%s"', "GEO R Code Layers", codeProjectId))
	if(is.null(result) || nrow(result) != 1L){
		msg <- sprintf("could not find R code dataset in project %s", inputDataMap[["parentId"]])
		finishWorkflowTask(list(status=kErrorStatusCode,msg=msg))
		setWorkFlowStatusAnnotation(dsId, kErrorStatusCode, msg)
		stop(msg)
	}
	datasetId <- result$dataset.id
	
	##########################################
	## get the entity ids for the code modules
	##########################################
	
	scriptName <- "run Metageo QC"
	result <- synapseQuery(sprintf('select * from layer where layer.name == "%s" and layer.parentId == "%s"', scriptName, datasetId))
	if(nrow(result) != 1L){
		msg <- sprintf("could not find R code entity %s in dataset %s", scriptName, datasetId)
		finishWorkflowTask(list(status=kErrorStatusCode,msg=msg))
		setWorkFlowStatusAnnotation(dsId, kErrorStatusCode, msg)
		stop(msg)
	}
	kRunMetaGeoCodeEntityId <- result$layer.id
	
	## get add location code entity and add location for the geo id
	ans <- tryCatch({
				createLayer(dsId)
			},
			error = function(e){
				msg <- sprintf("Failed to create layer: %s", e)
				finishWorkflowTask(list(status=kErrorStatusCode,msg=msg))
				setWorkFlowStatusAnnotation(dsId, kErrorStatusCode, msg)
				stop(e)
			}
	)
	
	## run the metaGeo workflow
	ans <- tryCatch({
				runMetaGeoEntity <- loadEntity(kRunMetaGeoCodeEntityId)
				runMetaGeoEntity$objects$run(ans$layerId, inputDataMap[["lastUpdate"]])
			},
			error = function(e){
				msg <- sprintf("Failed to run metaGeo workflow: %s", e)
				finishWorkflowTask(list(status=kErrorStatusCode,msg=msg))
				setWorkFlowStatusAnnotation(dsId, kErrorStatusCode, msg)
				stop(e)
			}
	)
	
	endtime<-proc.time()
	elapsedtime<-(endtime-starttime)["elapsed"]
	
	# max mem, megabytes
	maxmem<-sum(gc()[,6])
	
	## call the finish workflow step code
	msg <- sprintf("Successfully added GEO study %s to Synapse.", inputDataMap[['name']])
	finishWorkflowTask(
			output=list(
					status=kOkStatusCode, 
					msg=msg, 
					datasetId=dsId, 
					qcdExprLayerId=ans$exprLayers, 
					metadataLayerId=ans$metadataLayers,
					elapsedtime=elapsedtime,
					maxmem=maxmem
			)
	)
	setWorkFlowStatusAnnotation(dsId, kOkStatusCode, msg)
}
kOkStatusCode
