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
				kLastUpdateDateKey = 'character',
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
				kLastUpdateDateKey = "--lastUpdateDate",
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

getLastUpdateDateArg <- function() {
	constants <- new('SynapseWorkflowConstants')
	getArgVal(argName=constants@kLastUpdateDateKey) 
}

getInputLayerIdArg <- function() {
	constants <- new('SynapseWorkflowConstants')
	getArgVal(argName=constants@kInputLayerIdKey) 
}

getProjectId <- function() {
	constants <- new('SynapseWorkflowConstants')
	getArgVal(argName=constants@kProjectId) 
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
