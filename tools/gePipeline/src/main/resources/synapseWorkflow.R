library(methods)
setClass(
		Class = 'SynapseWorkflowConstants', 
		representation(
				kUsage = 'character',
				kUsernameKey = 'character',
				kPasswordKey = 'character',
				kSecretKey = 'character',
				kAuthEndpointKey = 'character',
				kRepoEndpointKey = 'character',
				kInputDatasetIdKey = 'character',
				kInputDataKey = 'character',
				kOutputKey = 'character',
				kWorkflowDone = 'character',
				kOutputStartDelimiterPattern = 'character',
				kOutputEndDelimiterPattern = 'character',
				kInputProjectIdKey = 'character',
				kMaxDatasetSizeArg = 'character'
		),
		prototype = prototype(
				kUsage = 'Usage: R myScript.r --args --username you@yourEmailAddress --password YourSynapsePassword --datasetId 42 --layerId 99',
				kUsernameKey = '--username',
				kPasswordKey = '--password',
				kSecretKey = '--secretKey',
				kAuthEndpointKey = '--authEndpoint',
				kRepoEndpointKey = '--repoEndpoint',
				kInputDatasetIdKey = '--datasetId',
				kInputDataKey = "--inputData",
				kOutputKey = 'output',
				kWorkflowDone = 'workflowDone',
				kOutputStartDelimiterPattern = 'SynapseWorkflowResult_START',
				kOutputEndDelimiterPattern = 'SynapseWorkflowResult_END',
				kInputProjectIdKey = '--projectId',
				kMaxDatasetSizeArg = '--maxDatasetSize'
		)
)

getUsernameArg <- function() {
	constants <- new('SynapseWorkflowConstants')
	getArgVal(argName=constants@kUsernameKey) 
}

getPasswordArg <- function() {
	constants <- new('SynapseWorkflowConstants')
	getArgVal(argName=constants@kPasswordKey) 
}

getSecretKeyArg <- function() {
	constants <- new('SynapseWorkflowConstants')
	getArgVal(argName=constants@kSecretKey) 
}

getAuthEndpointArg <- function() {
	constants <- new('SynapseWorkflowConstants')
	getOptionalArgVal(argName=constants@kAuthEndpointKey) 
}

getRepoEndpointArg <- function() {
	constants <- new('SynapseWorkflowConstants')
	getOptionalArgVal(argName=constants@kRepoEndpointKey) 
}

getInputDatasetIdArg <- function() {
	constants <- new('SynapseWorkflowConstants')
	getArgVal(argName=constants@kInputDatasetIdKey) 
}

getInputDataArg <- function() {
	constants <- new('SynapseWorkflowConstants')
	getArgVal(argName=constants@kInputDataKey) 
}

getInputLayerIdArg <- function() {
	constants <- new('SynapseWorkflowConstants')
	getArgVal(argName=constants@kInputLayerIdKey) 
}

getProjectId <- function() {
	constants <- new('SynapseWorkflowConstants')
	getArgVal(argName=constants@kInputProjectIdKey) 
}

getArgVal <- function(argName){
	constants <- new('SynapseWorkflowConstants')
	args <- commandArgs(trailingOnly = TRUE)
	indx <- which(args == argName)
	if(length(indx) == 1) 
		return(args[indx + 1])
	stop(constants@kUsage, indx)
}

getOptionalArgVal <- function(argName){
  constants <- new('SynapseWorkflowConstants')
	args <- commandArgs(trailingOnly = TRUE)
	indx <- which(args == argName)
	if(length(indx) == 1) 
		return(args[indx + 1])
	return(NULL)
}

finishWorkflowTask <- function(output) {
	constants <- new('SynapseWorkflowConstants')
	
	result <- list()
	result[[constants@kOutputKey]] <- output
	write(constants@kOutputStartDelimiterPattern, stdout())
	write(RJSONIO::toJSON(result), stdout())
	write(constants@kOutputEndDelimiterPattern, stdout())
}

skipWorkflowTask <- function(reason = 'this script does not want to work on this task') {
	constants <- new('SynapseWorkflowConstants')
	warning(reason)
	finishWorkflowTask(output=constants@kWorkflowDone)
	q()
}

getProjectIdArg <- function(){
	constants <- new('SynapseWorkflowConstants')
	getArgVal(argName=constants@kInputProjectIdKey) 
}

getMaxDatasetSizeArg <- function(){
	constants <- new('SynapseWorkflowConstants')
	getArgVal(argName=constants@kMaxDatasetSizeArg) 
}

# 'sourceRepositoryName', e.g. 'nbci'
createLayer <- function (datasetId, url, zipFilePath, layerName) 
{
	locationName<-layerName
	require(synapseClient)
	dataset <- getEntity(datasetId)
	# name used to be sprintf("%s_rawExpression", propertyValue(dataset, "name"))
	layer <- Layer(list(name = layerName, parentId = datasetId, type = "E", status = "raw"))
	qryString <- sprintf("select * from layer where layer.parentId == \"%s\" and layer.name == \"%s\"", 
			propertyValue(layer, "parentId"), propertyValue(layer, 
					"name"))
	qryResult <- synapseQuery(qryString)
	if (!is.null(qryResult)) 
		layer <- getEntity(qryResult$layer.id[1])
	annotValue(layer, "format") <- "GEO"
	if (is.null(propertyValue(layer, "id"))) {
		layer <- createEntity(layer)
	}
	else {
		layer <- updateEntity(layer)
	}
	## as we become 'generic', we cannot assume the layer comes from GEO
	## if (missing(url)) {
	##     geoId <- propertyValue(dataset, "name")
	##     url <- sprintf("ftp://ftp.ncbi.nih.gov/pub/geo/DATA/supplementary/series/%s/%s_RAW.tar", 
	##             geoId, geoId)
	## }
	if (missing(zipFilePath)) {
		zipFilePath <- synapseClient:::.curlWriterDownload(url = url)
	}
	if (!file.exists(zipFilePath)) {
		stop(sprintf("File not found: %s", zipFilePath))
	}
	parsedUrl <- synapseClient:::.ParsedUrl(url)
	destfile <- file.path(synapseCacheDir(), gsub("^/", "", parsedUrl@path))
	destfile <- path.expand(destfile)
	destdir <- gsub(parsedUrl@file, "", destfile, fixed = TRUE)
	destdir <- gsub("[\\/]+$", "", destdir)
	if (!file.exists(destdir)) 
		dir.create(destdir, recursive = TRUE)
	if (zipFilePath != destfile) 
		file.copy(zipFilePath, destfile, overwrite = TRUE)
	checksum <- as.character(tools::md5sum(destfile))
	qryString <- sprintf("select * from location where location.parentId == \"%s\" and location.name == \"%s\"", 
			propertyValue(layer, "id"), locationName)
	qryResult <- synapseQuery(qryString)
	location <- tryCatch({
				if (is.null(qryResult)) {
					location <- synapseClient:::Location(list(name = locationName, 
									parentId = propertyValue(layer, "id"), path = url, 
									md5sum = checksum, type = "external"))
					createEntity(location)
				}
				else {
					location <- getEntity(qryResult$location.id)
					propertyValue(location, "path") <- url
					propertyValue(location, "md5sum") <- checksum
					propertyValue(location, "type") <- "external"
					updateEntity(location)
				}
			}, error = function(e) {
				cat(sprintf("unable to create or update location for layer %s. Deleting data files\n", 
								propertyValue(layer, "id")))
				unlink(destdir, recursive = TRUE)
				stop(e)
			})
	list(layerId = propertyValue(layer, "id"), locationId = propertyValue(location, 
					"id"))
}

createDataset <- function (dsProperties, dsAnnotations) 
{
	if (!is.list(dsProperties) || !is.list(dsAnnotations)) 
		stop("Annotations and properties must both be submitted as lists.")
	require(synapseClient)
	dataset <- Dataset(dsProperties)
	annotationValues(dataset) <- dsAnnotations
	origTimestamp <- NULL
	qryString <- sprintf("select * from dataset where dataset.parentId == \"%s\" and dataset.name == \"%s\"", 
			propertyValue(dataset, "parentId"), propertyValue(dataset, 
					"name"))
	qryResult <- synapseQuery(qryString)
	if (!is.null(qryResult)) {
		dataset <- getEntity(qryResult$dataset.id[1])
		## origTimestamp <- annotValue(dataset, "lastUpdate")
		## origErrorCode <- annotValue(dataset, "workflowStatusCode")
		## annotationValues(dataset) <- dsAnnotations
		## dataset <- updateEntity(dataset)
	}
	else {
		dataset <- createEntity(dataset)
	}
	retVal <- list(projectId = propertyValue(dataset, "parentId"), dataset = dataset)
	## # if there are no CEL files OR 
	## # if the update date has not changed AND the previous error code is zero
	## # then don't rerun QC
	## if (
	##     (
	##                 !is.null(origTimestamp)
	##                 && !is.null(annotValue(dataset, "lastUpdate")) 
	##                 && annotValue(dataset, "lastUpdate") == origTimestamp
	##                 )
	##             &&
	##             (
	##                 !is.null(origErrorCode)
	##                 && as.numeric(origErrorCode) == 0
	##                 )
	##             )
	## {
	##     retVal$update <- FALSE
	##     retVal$reason <- sprintf("Dataset %s has not changed since last update.", propertyValue(dataset, "id"))
	## }
	## if (!is.null(dsAnnotations$hasCelFiles)
	##             && !is.na(as.logical(dsAnnotations$hasCelFiles))
	##             && !as.logical(dsAnnotations$hasCelFiles))
	## {
	##     retVal$update <- FALSE
	##     retVal$reason <- "Dataset has no expression data to process."
	## }
	retVal
}

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

lastUpdateAnnotName <- function(layer) {
	paste(layer, "lastUpdate", sep="_")
}

md5sumAnnotName <- function(layer) {
	paste(layer, "md5sum", sep="_")
}

setWorkFlowStatusAnnotation <- function(dsId, statusCode, statusMessage){
	ds <- getEntity(dsId)
	annotValue(ds, "workflowStatusCode") <- statusCode
	annotValue(ds, "workflowStatusMsg") <- statusMessage
	invisible(updateEntity(ds))
}

