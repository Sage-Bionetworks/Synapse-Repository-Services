# If you change anything here, be sure to update
# https://sagebionetworks.jira.com/svn/PLFM/trunk/tools/tcgaWorkflow/src/main/java/org/sagebionetworks/workflow/activity/Processing.java

setClass(
         Class = 'SynapseWorkflowConstants', 
         representation(
                        kUsage = 'character',
						kUsernameKey = 'character',
						kPasswordKey = 'character',
						kInputLayerIdKey = 'character',
                        kInputDatasetIdKey = 'character',
                        kInputLocalFilepathKey = 'character',
                        kOutputLayerIdKey = 'character',
                        kOutputStartDelimiterPattern = 'character',
                        kOutputEndDelimiterPattern = 'character'
                        ),
         prototype = prototype(
           kUsage = 'Usage: R script.r --args --username you@yourEmailAddress --password YourSynapsePassword --layerId 42 --localFilepath ./foo.txt',
		   kUsernameKey = '--username',
		   kPasswordKey = '--password',
		   kInputDatasetIdKey = '--datasetId',
           kInputLayerIdKey = '--layerId',
           kInputLocalFilepathKey = '--localFilepath',
           kOutputLayerIdKey = 'layerId',
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

getLocalFilepathArg <- function() {
  constants <- new('SynapseWorkflowConstants')
  getArgVal(argName=constants@kInputLocalFilepathKey) 
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

finishWorkflowTask <- function(outputLayerId) {
  constants <- new('SynapseWorkflowConstants')

  result <- list()
  result[constants@kOutputLayerIdKey] <- outputLayerId
  write(constants@kOutputStartDelimiterPattern, stdout())
  write(toJSON(result), stdout())
  write(constants@kOutputEndDelimiterPattern, stdout())
}

skipWorkflowTask <- function(reason = 'this script does not want to work on this task') {
	warning(reason)
	setOutputLayerId(outputLayerId=-1)
	q()
}