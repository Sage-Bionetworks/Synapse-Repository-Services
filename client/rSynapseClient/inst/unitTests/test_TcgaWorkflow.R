.setUp <- function() {
  # Override commandArgs
  commandArgs <- function () {
    c('--layerId', '42', '--localFilepath', './foo.txt' )
  }
}
.tearDown <- function() {
  # Do some test cleanup stuff here, if applicable
}

## unitTestGetLocalFilepath <- function() {
##   path <- getLocalFilepath()
##   checkEquals(path, './foo.txt')
## }

## unitTestGetInputLayerId <- function() {
##   layerId <- getInputLayerId()
##   checkEquals(layerId, 42)
## }

