# Testing object cache
# 
# Author: Matt Furia
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

unitTestCacheObject <-
		function()
{
	cacheDir <- file.path(synapseClient:::.getCache("synapseCacheDir"), synapseClient:::.getCache("rObjCacheDir"))
	object1 <- "foo"
	synapseClient:::.cacheObject("object1", envir=environment())
	checkTrue(file.exists(file.path(cacheDir, "object1.rbin")))
	
	env <- new.env()
	load(file.path(cacheDir, "object1.rbin"), envir=env)
	checkEquals(object1, env$object1)
}

unitTestTmpCacheObject <-
		function()
{
	cacheDir <- file.path(synapseClient:::.getCache("synapseCacheDir"), synapseClient:::.getCache("rObjCacheDir"))
	object1 <- "foo"
	
	checkException(synapseClient:::.tmpCacheObject("object1"))
	synapseClient:::.cacheObject("object1", envir = environment())
	synapseClient:::.tmpCacheObject("object1")
	checkTrue(file.exists(file.path(cacheDir, "object1.rbin.tmp")))
	checkTrue(!file.exists(file.path(cacheDir, "object1.rbin")))
}

unitTestRenameFromTmp <-
		function()
{
	cacheDir <- file.path(synapseClient:::.getCache("synapseCacheDir"), synapseClient:::.getCache("rObjCacheDir"))
	object1 <- "foo"
	
	synapseClient:::.cacheObject("object1", envir=environment())
	synapseClient:::.tmpCacheObject("object1")
	
	object2 <- object1
	synapseClient:::.renameCacheObjectFromTmp("object1", "object2")
	checkTrue(!file.exists(file.path(cacheDir, "object1.rbin.tmp")))
	checkTrue(file.exists(file.path(cacheDir, "object2.rbin")))
	
	env <- new.env()
	load(file.path(cacheDir, "object2.rbin"), envir = env)
	checkEquals(object1, env$object1)
	
}

unitTestDeleteTmpFile <-
		function()
{
	cacheDir <- file.path(synapseClient:::.getCache("synapseCacheDir"), synapseClient:::.getCache("rObjCacheDir"))
	object1 <- "foo"
	
	synapseClient:::.cacheObject("object1", envir=environment())
	synapseClient:::.tmpCacheObject("object1")
	
	synapseClient:::.deleteTmpCacheFile("object1")
	checkTrue(!(file.exists(file.path(cacheDir, "object1.rbin.tmp"))))
}

unitTestDeleteCacheFile <-
		function()
{
	cacheDir <- file.path(synapseClient:::.getCache("synapseCacheDir"), synapseClient:::.getCache("rObjCacheDir"))
	object1 <- "foo"
	
	synapseClient:::.cacheObject("object1", envir=environment())
	synapseClient:::.deleteCacheFile("object1")
	checkTrue(!(file.exists(file.path(cacheDir, "object1.rbin"))))
}

unitTestLoadCachedObjects <-
		function()
{
	cacheDir <- file.path(synapseClient:::.getCache("synapseCacheDir"), synapseClient:::.getCache("rObjCacheDir"))
	object1 <- "foo"
	object2 <- diag(nrow=10,ncol=10)
	
	synapseClient:::.cacheObject("object1", envir=environment())
	synapseClient:::.cacheObject("object2", envir=environment())
	
	env <- new.env()
	synapseClient:::.loadCachedObjects(env)
	
	checkTrue(all(object2 == env$object2))
	checkEquals(object1, env$object1)
}

