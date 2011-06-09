.setUp <- function() {
  # Override commandArgs
  myCommandArgs <- function () {
    c('--datasetId','23', '--layerId', '42', '--localFilepath', './foo.txt' )
  }
  
  attr(myCommandArgs, "origFCN") <- base:::commandArgs
  assignInNamespace("commandArgs", myCommandArgs, "base")
}
.tearDown <- function() {
	assignInNamespace("commandArgs", attr(base:::commandArgs, "origFCN"), "base")
}

unitTestGetInputDatasetId <- function() {
	datasetId <- getInputDatasetId()
	checkEquals(datasetId, '23')
}

 unitTestGetLocalFilepath <- function() {
   path <- getLocalFilepath()
   checkEquals(path, './foo.txt')
 }

 unitTestGetInputLayerId <- function() {
   layerId <- getInputLayerId()
   checkEquals(layerId, '42')
 }

