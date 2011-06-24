.setUp <- function() {
  # Override commandArgs
  myCommandArgs <- function (trailingOnly = TRUE) {
    c('--args', '--username', 'foo', '--password', 'bar', '--datasetId', '23', 
			'--layerId', '42' )
  }
  
  attr(myCommandArgs, "origFCN") <- base:::commandArgs
  assignInNamespace("commandArgs", myCommandArgs, "base")
}
.tearDown <- function() {
	assignInNamespace("commandArgs", attr(base:::commandArgs, "origFCN"), "base")
}

unitTestGetUsernameArg <- function() {
	username <- getUsernameArg()
	checkEquals(username, 'foo')
}

unitTestGetPasswordArg <- function() {
	password <- getPasswordArg()
	checkEquals(password, 'bar')
}

unitTestGetInputDatasetIdArg <- function() {
	datasetId <- getInputDatasetIdArg()
	checkEquals(datasetId, '23')
}

 unitTestGetInputLayerIdArg <- function() {
   layerId <- getInputLayerIdArg()
   checkEquals(layerId, '42')
 }

unitTestFinishWorkflowTask <- function() {
	finishWorkflowTask(outputLayerId=-999)
}

unitTestFinishWorkflowTaskBadArgs <- function() {
	checkException(finishWorkflowTask())
}