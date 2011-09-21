# Testing object cache
# 
# Author: Matt Furia
###############################################################################

#.setUp <-
#		function()
#{
#	synapseClient:::.setCache("oldSynapseCacheDir", synapseClient:::.getCache("synapseCacheDir"))
#	synapseClient:::.setCache("synapseCacheDir", tempfile())
#}
#
#.tearDown <-
#		function()
#{
#	unlink(synapseClient:::.getCache("synapseCacheDir"), recursive = TRUE)
#	synapseClient:::.setCache("synapseCacheDir", synapseClient:::.getCache("oldSynapseCacheDir"))
#	synapseClient:::.deleteCache("oldSynapseCacheDir")
#}

unitTestCacheObject <-
		function()
{
	layer <- new("Layer")
	cacheDir <- file.path(layer$cacheDir, synapseClient:::.getCache("rObjCacheDir"))
	layer@objects$object1 <- "foo"
	synapseClient:::.cacheObject(layer, "object1")
	checkTrue(file.exists(file.path(cacheDir, "object1.rbin")))
	
	env <- new.env()
	load(file.path(cacheDir, "object1.rbin"), envir=env)
	checkEquals(layer$objects$object1, env$object1)
}

unitTestTmpCacheObject <-
		function()
{
	layer <- new("Layer")
	cacheDir <- file.path(layer$cacheDir, synapseClient:::.getCache("rObjCacheDir"))
	assign("object1", "foo", envir = layer@objects)
	
	checkException(synapseClient:::.tmpCacheObject("object1"))
	synapseClient:::.cacheObject(layer, "object1")
	synapseClient:::.tmpCacheObject(layer, "object1")
	checkTrue(file.exists(file.path(cacheDir, "object1.rbin.tmp")))
	checkTrue(!file.exists(file.path(cacheDir, "object1.rbin")))
}

unitTestRenameFromTmp <-
		function()
{
	layer <- new("Layer")
	cacheDir <- file.path(layer$cacheDir, synapseClient:::.getCache("rObjCacheDir"))
	
	object1 <- "foo"
	assign("object1", object1, envir = layer@objects)
	
	synapseClient:::.cacheObject(layer, "object1")
	synapseClient:::.tmpCacheObject(layer, "object1")
	
	assign("object2", object1, envir = layer@objects)
	synapseClient:::.renameCacheObjectFromTmp(layer, "object1", "object2")
	checkTrue(!file.exists(file.path(cacheDir, "object1.rbin.tmp")))
	checkTrue(file.exists(file.path(cacheDir, "object2.rbin")))
	
	env <- new.env()
	load(file.path(cacheDir, "object2.rbin"), envir = env)
	checkEquals(object1, env$object1)
	
}

unitTestDeleteTmpFile <-
		function()
{
	layer <- new("Layer")
	cacheDir <- file.path(layer$cacheDir, synapseClient:::.getCache("rObjCacheDir"))
	assign("object1", "foo", envir = layer@objects)
	
	synapseClient:::.cacheObject(layer, "object1")
	synapseClient:::.tmpCacheObject(layer, "object1")
	
	synapseClient:::.deleteTmpCacheFile(layer, "object1")
	checkTrue(!(file.exists(file.path(cacheDir, "object1.rbin.tmp"))))
}

unitTestDeleteCacheFile <-
		function()
{
	layer <- new("Layer")
	cacheDir <- file.path(layer$cacheDir, synapseClient:::.getCache("rObjCacheDir"))
	assign("object1", "foo", envir = layer@objects)
	
	synapseClient:::.cacheObject(layer, "object1")
	synapseClient:::.deleteCacheFile(layer, "object1")
	checkTrue(!(file.exists(file.path(cacheDir, "object1.rbin"))))
}

unitTestLoadCachedObjects <-
		function()
{
	layer <- new("Layer")
	cacheDir <- file.path(layer$cacheDir, synapseClient:::.getCache("rObjCacheDir"))
	object1 <- "foo"
	object2 <- diag(nrow=10,ncol=10)

	assign("object1", object1, envir = layer@objects)
	assign("object2", object2, envir = layer@objects)
	
	synapseClient:::.cacheObject(layer, "object1")
	synapseClient:::.cacheObject(layer, "object2")
	
	layer@objects <- new.env()
	checkEquals(length(layer$objects), 0L)
	synapseClient:::.loadCachedObjects(layer)
	checkEquals(length(layer$objects), 2L)
	
	checkTrue(all(object2 == layer$objects$object2))
	checkEquals(object1, layer$objects$object1)
}


