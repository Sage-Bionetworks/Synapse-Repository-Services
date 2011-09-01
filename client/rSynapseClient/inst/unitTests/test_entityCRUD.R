.setUp <- function() {
	## make a copy of the old cache
	oldCache <- synapseClient:::.cache
	newCache <- new.env(parent=parent.env(oldCache))
	for(key in ls(oldCache))
		assign(key,get(key,envir=oldCache), envir=newCache)
	
	# Override getURL to not actually make a remote call
	myGetURL <- function (url, ..., .opts = list(), write = basicTextGatherer(), 
			curl = getCurlHandle(), async = length(url) > 1, .encoding = integer()) {
		sampleResponse <- '{"results":"this is a fake response"}'
		return(sampleResponse)
	}
	
	# Override .checkCurlResponse with a do-nothing function
	myCheckCurlResponse <- function(object,response) {}
			
	## unload package namespaces so their functions can be overridden
	unloadNamespace("synapseClient")
	unloadNamespace("RCurl")
	assignInNamespace("getURL", myGetURL, "RCurl")
	assignInNamespace(".checkCurlResponse", myCheckCurlResponse, "synapseClient")
	assignInNamespace(".cache", newCache, "synapseClient")
	attachNamespace("synapseClient")
	
	.setCache("oldCache", oldCache)

}

.tearDown <- function() {
	oldCache <- .getCache("oldCache")
	# put back the overridden functions and original cache
	unloadNamespace("synapseClient")
	unloadNamespace("RCurl")
	assignInNamespace(".cache", oldCache, "synapseClient")
	attachNamespace("synapseClient")
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