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

  args <- commandArgs()
  for(i in 1:length(args)) {
    if(args[i] == constants@kInputLocalFilepathKey) {
      return(args[i+1])
    }
  }
  stop(constants@kUsage)
}

getInputDatasetId <- function() {
  constants <- new('TcgaWorkflowConstants')

  args <- commandArgs()
  for(i in 1:length(args)) {
    if(args[i] == constants@kInputDatasetIdKey) {
      return(args[i+1])
    }
  }
  stop(constants@kUsage)
}

getInputLayerId <- function() {
  constants <- new('TcgaWorkflowConstants')

  args <- commandArgs()
  for(i in 1:length(args)) {
    if(args[i] == constants@kInputLayerIdKey) {
      return(args[i+1])
    }
  }
  stop(constants@kUsage)
}

setOutputLayerId <- function(layerId) {
  constants <- new('TcgaWorkflowConstants')

  result <- list()
  result[constants@kOutputLayerIdKey] <- layerId
  write(constants@kOutputStartDelimiterPattern, stdout())
  write(toJSON(result), stdout())
  write(constants@kOutputEndDelimiterPattern, stdout())
}

