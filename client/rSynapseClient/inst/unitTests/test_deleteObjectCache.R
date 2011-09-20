# TODO: Add comment
# 
# Author: furia
###############################################################################

.setUp <-
		function()
{
	synapseClient:::.setCache("oldSynapseCacheDir", synapseClient:::.getCache("synapseCacheDir"))
	synapseClient:::.setCache("synapseCacheDir", tempfile())
}

.tearDown <-
		function()
{
	unlink(synapseClient:::.getCache("synapseCacheDir"), recursive = TRUE)
	synapseClient:::.setCache("synapseCacheDir", synapseClient:::.getCache("oldSynapseCacheDir"))
	synapseClient:::.deleteCache("oldSynapseCacheDir")
}

unitTestDelete <-
		function()
{
	layer <- new(Class="Layer")
	cacheDir <- file.path(synapseClient:::.getCache("synapseCacheDir"), synapseClient:::.getCache("rObjCacheDir"))
	
	object1 <- "foo"
	addObject(layer, object1)
	deleteObject(layer, "object1")
	
	checkTrue(!file.exists(file.path(cacheDir, "object1.rbin")))
	
}

unitTestDeleteMultiple <-
		function()
{
	layer <- new(Class="Layer")
	cacheDir <- file.path(synapseClient:::.getCache("synapseCacheDir"), synapseClient:::.getCache("rObjCacheDir"))
	
	object1 <- "foo"
	object2 <- "bar"
	addObject(layer, object1)
	addObject(layer, object2)
	deleteObject(layer, c("object1", "object2"))
	
	checkTrue(!file.exists(file.path(cacheDir, "object1.rbin")))
	checkTrue(!file.exists(file.path(cacheDir, "object2.rbin")))
}

