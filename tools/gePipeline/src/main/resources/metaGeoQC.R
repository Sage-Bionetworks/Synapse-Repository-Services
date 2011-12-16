#!/usr/bin/env Rscript

###############################################################################
# Run the metaGeo QC and load the results to Synapse
# 
# params:
#   userName - Synapse (service) account used to run the workflow
#   secretKey - API key for 'userName', used to authenticate requests to Synapse
#   authEndpoint - web address of authentication server
#   repoEndpoint - web address of repository server
#   urlEncodedInputData - dataset parameters:
#		parentId (REQUIRED) - ID of the Synapse project for the datasets of processed data
#		name (REQUIRED) - name of dataset which will contain the output of this function
#		lastUpdate (REQUIRED) - time stamp on data source, indicating when last changed
#   	sourceLayerId (REQUIRED) - the source layer to be processed
#  		layerName (REQUIRED) - the name of the created layer
#		number_of_samples
#		description
#		status
#		createdBy
#	
# 
# Author: Matt Furia
###############################################################################

metaGeoQC<-function(userName, secretKey, authEndpoint, repoEndpoint, urlEncodedInputData)
{
	
	Sys.setenv("R_ZIPCMD"="/usr/bin/zip")
	
	recordProvenance <- FALSE
	
	starttime<-proc.time()
	
	## constants
	kErrorStatusCode <- 1L
	kOkStatusCode <- 0L
	
	## load synapse client
	library(synapseClient)
	
	# override these two lines to set where temporary files go
	#synapseCacheDir("/mnt/ebs/synapseCacheDir")
	#Sys.setenv(TMPDIR="/mnt/ebs/r_tmp")
	
	
	## load the workflow utilities
	source('./src/main/resources/synapseWorkflow.R')
	source('./src/main/resources/doMetaGeoQc.R')
	
	# to avoid problems with spaces, quotes, etc. we just URLEncode the input data
	# thus we decode it here
	inputData <- URLdecode(urlEncodedInputData)
	inputDataMap<-RJSONIO::fromJSON(inputData, simplify=F)
	
	
	# divides attributes into 'properties' and 'annotations'
	splitDatasetAttributes<-function(a) {
		dataSetPropertyLabels<-c("name", "description", "status", "createdBy", "parentId")
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
	
	indx <- which(tolower(names(inputDataMap)) == "number_of_samples")
	if(length(indx) > 0L)
		names(inputDataMap)[indx] <- tolower(names(inputDataMap)[indx])
	
	attributes<-splitDatasetAttributes(inputDataMap[setdiff(names(inputDataMap),"lastUpdate")])
	
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
	timestamp <- gsub("-", "_", gsub(":", ".", Sys.time()))
	
	if (recordProvenance){
		analysisDescription <- paste("Unsupervised QC for", inputDataMap[["name"]], timestamp)
		analysis <- Analysis(list(description=analysisDescription, 
					name=analysisDescription, parentId=inputDataMap[["parentId"]]))
		analysis <- createEntity(analysis)
		# this function no longer does the 'indexing' step
		## analysisStep<-startStep(analysis)
		## propertyValue(analysisStep, "name")<-paste("GEO indexing step for ", inputDataMap[["name"]], timestamp)
		## analysisStep <- updateEntity(analysisStep)
		## stopStep()
		analysisStep<-startStep(analysis)	
		propertyValue(analysisStep, "name")<-paste("Unsupervised QC step for ", inputDataMap[["name"]], timestamp)
		analysisStep <- updateEntity(analysisStep)
	}
	
	
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
	msg <- "QC In Progress"

	# we initially set the status code to ERROR.  Upon successful completion we'll change it to OK
	setWorkFlowStatusAnnotation(dsId, kErrorStatusCode, msg)
	
	if(!ans$update){
		msg <- ans$reason
		finishWorkflowTask(list(status=kOkStatusCode, msg=msg, datasetId=dsId))
		setWorkFlowStatusAnnotation(dsId, kOkStatusCode, msg)
	}else{
		
		## 'Indexing' is now done upstream, not by this function
		##
		## ## get add location code entity and add location for the geo id
		## ans <- tryCatch({
		##             createLayer(dsId, inputDataMap[["url"]], inputDataMap[["layerName"]])
		##         },
		##         error = function(e){
		##             msg <- sprintf("Failed to create layer: %s", e)
		##             finishWorkflowTask(list(status=kErrorStatusCode,msg=msg))
		##             setWorkFlowStatusAnnotation(dsId, kErrorStatusCode, msg)
		##             stop(e)
		##         }
		## )
		
		## for now, disable the use of code object in Synapse
		## ##########################################
		## ## get the entity ids for the code modules
		## ##########################################
		## codeProjectId <- inputDataMap[["parentId"]] # can override if the code is in another project
		## 
		## scriptName <- "run Metageo QC"
		## result <- synapseQuery(sprintf('select * from code where code.name == "%s" and code.parentId == "%s"', scriptName, codeProjectId))
		## if(nrow(result) != 1L){
		##     msg <- sprintf("could not find R code entity %s in dataset %s", scriptName, codeProjectId)
		##     finishWorkflowTask(list(status=kErrorStatusCode,msg=msg))
		##     setWorkFlowStatusAnnotation(dsId, kErrorStatusCode, msg)
		##     stop(msg)
		## }
		## kRunMetaGeoCodeEntityId <- result$code.id
		
		## run the metaGeo workflow
		ans <- tryCatch({
					sourceLayerId <-inputDataMap[["sourceLayerId"]]
					## for now, disable the use of code object in Synapse
					## runMetaGeoEntity <- loadEntity(kRunMetaGeoCodeEntityId)
					## runMetaGeoEntity$objects$run(sourceLayerId, name, inputDataMap[["lastUpdate"]])
					doMetaGeoQc(sourceLayerId, name, inputDataMap[["lastUpdate"]])
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
	if (recordProvenance) {
		stopStep()
	}
	return(kOkStatusCode)
}
