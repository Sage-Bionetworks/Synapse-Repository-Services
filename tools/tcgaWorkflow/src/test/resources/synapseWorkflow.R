# If you change anything here, be sure to update the corresponding Java workflow code
# https://sagebionetworks.jira.com/svn/PLFM/trunk/tools/tcgaWorkflow/src/main/java/org/sagebionetworks/workflow/activity/Processing.java
# https://sagebionetworks.jira.com/svn/PLFM/trunk/tools/tcgaWorkflow/src/main/java/org/sagebionetworks/workflow/activity/Constants.java

# TODO this R code needs to be completely rewritten
# Consider using a better arg parser http://stackoverflow.com/questions/2151212/how-can-i-read-command-line-parameters-from-an-r-script

setClass(
		Class = 'SynapseWorkflowConstants', 
		representation(
				kUsage = 'character',
				kUsernameKey = 'character',
				kPasswordKey = 'character',
				kAuthEndpointKey = 'character',
				kRepoEndpointKey = 'character',
				kInputLayerIdKey = 'character',
				kInputDatasetIdKey = 'character',
				kOutputLayerIdKey = 'character',
				kWorkflowDone = 'character',
				kOutputStartDelimiterPattern = 'character',
				kOutputEndDelimiterPattern = 'character'
		),
		prototype = prototype(
				kUsage = 'Usage: R script.r --args --username you@yourEmailAddress --password YourSynapsePassword --layerId 42',
				kUsernameKey = '--username',
				kPasswordKey = '--password',
				kAuthEndpointKey = '--authEndpoint',
				kRepoEndpointKey = '--repoEndpoint',
				kInputDatasetIdKey = '--datasetId',
				kInputLayerIdKey = '--layerId',
				kOutputLayerIdKey = 'layerId',
				kWorkflowDone = 'workflowDone',
				kOutputStartDelimiterPattern = 'SynapseWorkflowResult_START',
				kOutputEndDelimiterPattern = 'SynapseWorkflowResult_END'
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

getInputLayerIdArg <- function() {
	constants <- new('SynapseWorkflowConstants')
	getArgVal(argName=constants@kInputLayerIdKey) 
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

finishWorkflowTask <- function(outputLayerId) {
	constants <- new('SynapseWorkflowConstants')
	
	result <- list()
	result[constants@kOutputLayerIdKey] <- outputLayerId
	write(constants@kOutputStartDelimiterPattern, stdout())
	write(RJSONIO::toJSON(result), stdout())
	write(constants@kOutputEndDelimiterPattern, stdout())
}

skipWorkflowTask <- function(reason = 'this script does not want to work on this task') {
	constants <- new('SynapseWorkflowConstants')
	warning(reason)
	finishWorkflowTask(outputLayerId=constants@kWorkflowDone)
	q()
}