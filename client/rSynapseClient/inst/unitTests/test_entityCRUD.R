.setUp <- function() {
	# Do some setup stuff here like creating and populating a stub implementation of the repository service with some data
	
	# Override getURL to not actually make a remote call
	myGetURL <- function (url, ..., .opts = list(), write = basicTextGatherer(), 
			curl = getCurlHandle(), async = length(url) > 1, .encoding = integer()) {
		sampleResponse <- '{"results":"this is a fake response"}'
		return(sampleResponse)
	}
	
	# Override .checkCurlResponse with a do-nothing function
	myCheckCurlResponse <- function(object,response) {}
	
	#back up the old methods
	attr(myCheckCurlResponse, "origFcn") <- synapseClient:::.checkCurlResponse
	attr(myGetURL, "origFcn") <- RCurl:::getURL
	
	## detach packages so their functions can be overridden
	detach('package:synapseClient', force=TRUE)
	detach('package:RCurl', force=TRUE)
	assignInNamespace(".checkCurlResponse", myCheckCurlResponse, "synapseClient")
	assignInNamespace("getURL", myGetURL, "RCurl")
	
	#reload detached packages
	library(synapseClient, quietly=TRUE)
}

.tearDown <- function() {
	# Do some test cleanup stuff here, if applicable
	detach('package:synapseClient', force = TRUE)
	detach('package:RCurl', force = TRUE)
	assignInNamespace(".checkCurlResponse", attr(synapseClient:::.checkCurlResponse, "origFcn"), "synapseClient")
	assignInNamespace("getURL", attr(RCurl:::getURL, "origFcn"), "RCurl")
	library(synapseClient, quietly = TRUE)
}

unitTestCreateEntityInvalidParameters <- function() {
	checkException(createDataset())

	checkException(createDataset(entity='this is not an entity, it must be a list'))
}

unitTestGetEntityInvalidParameters <- function() {
	checkException(getDataset())
		
	invalidDataset <- list()
	invalidDataset$name <- 'I am missing a uri field'
	checkException(getDataset(entity=invalidDataset))
	
	tooManyIds <- c('123', '456', '789')
	checkException(getDataset(entity=tooManyIds))
}

unitTestUpdateEntityInvalidParameters <- function() {
	checkException(updateDataset())
	
	checkException(updateDataset(entity='this is not an entity, it must be a list'))
}

unitTestDeleteEntityInvalidParameters <- function() {
	checkException(deleteDataset())

	invalidDataset <- list()
	invalidDataset$name <- 'I am missing a uri field'
	checkException(deleteDataset(entity=invalidDataset))
	
	tooManyIds <- c('123', '456', '789')
	checkException(deleteDataset(entity=tooManyIds))
}

unitTestGetAnnotationsInvalidParameters <- function() {
	checkException(getAnnotations())

	invalidDataset <- list()
	invalidDataset$name <- 'I am missing an annotations uri field'
	checkException(getAnnotations(entity=invalidDataset))
}

unitTestUpdateAnnotationsInvalidParameters <- function() {
	checkException(getAnnotations())

	dataset <- list()
	dataset$uri <- '/dataset/123' 
	dataset$name <- 'I am a dataset, but you pass an annotations entity, not a dataset entity to updateAnnotations'
	checkException(getAnnotations(entity=dataset))
}