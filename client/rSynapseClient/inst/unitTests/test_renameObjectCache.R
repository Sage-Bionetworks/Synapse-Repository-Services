# 
# Author: Matt Furia
###############################################################################

unitTestRename <-
		function()
{
	layer <- new(Class="Layer")
	cacheDir <- file.path(layer$cacheDir, synapseClient:::.getCache("rObjCacheDir"))
	
	object1 <- "foo"
	addObject(layer, object1)
	
	renameObject(layer, "object1", "object2")
	checkTrue(!file.exists(file.path(cacheDir, "object1.rbin")))
	checkTrue(file.exists(file.path(cacheDir, "object2.rbin")))
	env <- new.env()
	load(file.path(cacheDir, "object2.rbin"), envir = env)
	checkEquals(object1, env$object2)
}

unitTestRenameSameName <-
		function()
{
	layer <- new(Class="Layer")
	cacheDir <- file.path(layer$cacheDir, synapseClient:::.getCache("rObjCacheDir"))
	
	object1 <- "foo"
	addObject(layer, object1)
	
	renameObject(layer, "object1", "object1")
	checkTrue(file.exists(file.path(cacheDir, "object1.rbin")))
	checkTrue(!file.exists(file.path(cacheDir, "object1.rbin.tmp")))
	env <- new.env()
	load(file.path(cacheDir, "object1.rbin"), envir = env)
	checkEquals(object1, env$object1)
}

unitTestRenameOverwrite <-
		function()
{
	layer <- new(Class="Layer")
	cacheDir <- file.path(layer$cacheDir, synapseClient:::.getCache("rObjCacheDir"))
	
	object1 <- "foo"
	object2 <- "bar"
	addObject(layer, object1)
	addObject(layer, object2)
	
	renameObject(layer, "object1", "object2")
	checkTrue(!file.exists(file.path(cacheDir, "object1.rbin")))
	checkTrue(file.exists(file.path(cacheDir, "object2.rbin")))
	checkTrue(!file.exists(file.path(cacheDir, "object1.rbin.tmp")))
	checkTrue(!file.exists(file.path(cacheDir, "object2.rbin.tmp")))
	env <- new.env()
	load(file.path(cacheDir, "object2.rbin"), envir = env)
	checkEquals(object1, env$object2)
}

unitTestRenameMultiple <-
		function()
{
	layer <- new(Class="Layer")
	cacheDir <- file.path(layer$cacheDir, synapseClient:::.getCache("rObjCacheDir"))
	
	object1 <- "foo"
	object2 <- "bar"
	addObject(layer, object1)
	addObject(layer, object2)
	
	renameObject(layer, c("object1","object2"), c("object3", "object4"))
	checkTrue(!file.exists(file.path(cacheDir, "object1.rbin")))
	checkTrue(!file.exists(file.path(cacheDir, "object2.rbin")))
	checkTrue(file.exists(file.path(cacheDir, "object3.rbin")))
	checkTrue(file.exists(file.path(cacheDir, "object4.rbin")))
	checkTrue(!file.exists(file.path(cacheDir, "object1.rbin.tmp")))
	checkTrue(!file.exists(file.path(cacheDir, "object2.rbin.tmp")))
	env <- new.env()
	load(file.path(cacheDir, "object3.rbin"), envir = env)
	load(file.path(cacheDir, "object4.rbin"), envir = env)
	checkEquals(object1, env$object3)
	checkEquals(object2, env$object4)
}

unitTestRenameMultipleOverwriteOne <-
		function()
{
	layer <- new(Class="Layer")
	cacheDir <- file.path(layer$cacheDir, synapseClient:::.getCache("rObjCacheDir"))
	
	object1 <- "foo"
	object2 <- "bar"
	addObject(layer, object1)
	addObject(layer, object2)
	
	renameObject(layer, c("object1","object2"), c("object2", "object5"))
	checkTrue(!file.exists(file.path(cacheDir, "object1.rbin")))
	checkTrue(file.exists(file.path(cacheDir, "object2.rbin")))
	checkTrue(file.exists(file.path(cacheDir, "object5.rbin")))
	checkTrue(!file.exists(file.path(cacheDir, "object1.rbin.tmp")))
	checkTrue(!file.exists(file.path(cacheDir, "object2.rbin.tmp")))
	env <- new.env()
	load(file.path(cacheDir, "object2.rbin"), envir = env)
	load(file.path(cacheDir, "object5.rbin"), envir = env)
	checkEquals(object1, env$object2)
	checkEquals(object2, env$object5)
}

unitTestRenameMultipleSwapNames <-
		function()
{
	layer <- new(Class="Layer")
	cacheDir <- file.path(layer$cacheDir, synapseClient:::.getCache("rObjCacheDir"))
	
	object1 <- "foo"
	object2 <- "bar"
	addObject(layer, object1)
	addObject(layer, object2)
	
	renameObject(layer, c("object1","object2"), c("object2", "object1"))
	checkTrue(file.exists(file.path(cacheDir, "object1.rbin")))
	checkTrue(file.exists(file.path(cacheDir, "object2.rbin")))
	checkTrue(!file.exists(file.path(cacheDir, "object1.rbin.tmp")))
	checkTrue(!file.exists(file.path(cacheDir, "object2.rbin.tmp")))
	env <- new.env()
	load(file.path(cacheDir, "object1.rbin"), envir = env)
	load(file.path(cacheDir, "object2.rbin"), envir = env)
	checkEquals(object1, env$object2)
	checkEquals(object2, env$object1)
}

