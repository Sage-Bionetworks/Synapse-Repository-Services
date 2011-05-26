.setUp <- function() {
  # Override commandArgs
  commandArgs <- function () {
    c('--datasetId 23 --layerId', '42', '--localFilepath', './foo.txt' )
  }
}
.tearDown <- function() {
  # Do some test cleanup stuff here, if applicable
}

# TODO this doesn't correctly override the definition of commandArgs()
## unitTestGetLocalFilepath <- function() {
##   path <- getLocalFilepath()
##   checkEquals(path, './foo.txt')
## }

## unitTestGetInputDatasetId <- function() {
##   datasetId <- getInputDatasetId()
##   checkEquals(datasetId, '23')
## }

## unitTestGetInputLayerId <- function() {
##   layerId <- getInputLayerId()
##   checkEquals(layerId, '42')
## }

