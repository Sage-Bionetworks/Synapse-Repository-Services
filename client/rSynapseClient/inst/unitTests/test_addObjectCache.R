# TODO: Add comment
# 
# Author: matt furia
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


unitTestAddCache <-
		function()
{
	layer <- new(Class="Layer")
	cacheDir <- file.path(synapseClient:::.getCache("synapseCacheDir"), synapseClient:::.getCache("rObjCacheDir"))
	
	object1 <- "foo"
	addObject(layer, object1)
	
	checkTrue(file.exists(file.path(cacheDir, "object1.rbin")))
	env <- new.env()
	load(file.path(cacheDir, "object1.rbin"))
	checkTrue(all(object1 == env$object1))
}

unitTestAddCacheMultiple <-
		function()
{
	layer <- new(Class="Layer")
	cacheDir <- file.path(synapseClient:::.getCache("synapseCacheDir"), synapseClient:::.getCache("rObjCacheDir"))
	
	object1 <- "foo"
	addObject(layer, object1)
	object2 <- "bar"
	addObject(layer, object2)
	
	checkTrue(file.exists(file.path(cacheDir, "object1.rbin")))
	checkTrue(file.exists(file.path(cacheDir, "object2.rbin")))
	
	env <- new.env()
	load(file.path(cacheDir, "object1.rbin"), envir = env)
	checkEquals(object1, env$object1, envir = env)
	env <- new.env()
	load(file.path(cacheDir, "object2.rbin"), envir = env)
	checkEquals(object2, env$object2, envir = env)
}
