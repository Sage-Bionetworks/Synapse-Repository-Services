# If you change anything here, be sure to update
# https://sagebionetworks.jira.com/svn/PLFM/trunk/tools/tcgaWorkflow/src/main/java/org/sagebionetworks/workflow/curation/activity/ProcessTcgaSourceLayer.java

# TODO figure out the right way to have constants that can be accessed
# by multiple methods
setClass(
         Class = 'TcgaWorkflowConstants', 
         representation(
                        kUsage = 'character',
                        kInputLayerIdKey = 'character',
                        kInputDatasetIdKey = 'character',
                        kInputLocalFilepathKey = 'character',
                        kOutputLayerIdKey = 'character',
                        kOutputStartDelimiterPattern = 'character',
                        kOutputEndDelimiterPattern = 'character'
                        ),
         prototype = prototype(
           kUsage = 'Usage: R script.r --layerId 42 --localFilepath ./foo.txt',
           kInputDatasetIdKey = '--datasetId',
           kInputLayerIdKey = '--layerId',
           kInputLocalFilepathKey = '--localFilepath',
           kOutputLayerIdKey = 'layerId',
           kOutputStartDelimiterPattern = 'TcgaWorkflowResult_START',
           kOutputEndDelimiterPattern = 'TcgaWorkflowResult_END'
           )
         )

getLocalFilepath <- function() {
  constants <- new('TcgaWorkflowConstants')
  getArgVal(constants@kInputLocalFilepathKey) 
}

getInputDatasetId <- function() {
  constants <- new('TcgaWorkflowConstants')
  getArgVal(constants@kInputDatasetIdKey) 
}

getInputLayerId <- function() {
  constants <- new('TcgaWorkflowConstants')
  getArgVal(constants@kInputLayerIdKey) 
}

getArgVal <- function(argName){
	constants <- new('TcgaWorkflowConstants')
	args <- commandArgs()
	indx <- which(args == argName)
	if(length(indx) == 1) 
		return(args[indx + 1])
	stop(constants@kUsage, indx)
}

setOutputLayerId <- function(layerId) {
  constants <- new('TcgaWorkflowConstants')

  result <- list()
  result[constants@kOutputLayerIdKey] <- layerId
  write(constants@kOutputStartDelimiterPattern, stdout())
  write(toJSON(result), stdout())
  write(constants@kOutputEndDelimiterPattern, stdout())
}

