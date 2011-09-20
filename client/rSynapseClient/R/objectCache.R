# Functions for manipulating the object cache 
# 
# Author: Matt Furia
###############################################################################


.cacheObject <-
		function(object, envir)
{
	destFile <- .generateCacheFileName(object)
	if(!file.exists(attr(destFile, "cacheDir")))
		dir.create(attr(destFile, "cacheDir"), recursive = TRUE)

	save(list = object, envir = envir, file = destFile)
}

.tmpCacheObject <- 
		function(object)
{
	if(!file.exists(.generateCacheFileName(object)))
		stop("source file does not exist")
	file.rename(.generateCacheFileName(object), .generateTmpCacheFileName(object))
}

.renameCacheObjectFromTmp <-
		function(origObject, newObject)
{
	file.rename(.generateTmpCacheFileName(origObject), .generateCacheFileName(newObject))
}

.generateCacheFileName <- 
		function(object, cacheRoot = .getCache("synapseCacheDir"), cacheDir = .getCache("rObjCacheDir"))
{
	kSuffix <- "rbin"
	filePath <- file.path(cacheRoot, cacheDir, sprintf("%s.%s", object, kSuffix))
	attr(filePath, "cacheDir") <- file.path(cacheRoot, cacheDir)
	filePath
}

.generateTmpCacheFileName <- 
		function(object, cacheRoot = .getCache("synapseCacheDir"), cacheDir = .getCache("rObjCacheDir"))
{
	kSuffix <- "rbin.tmp"
	filePath <- file.path(cacheRoot, cacheDir, sprintf("%s.%s", object, kSuffix))
	attr(filePath, "cacheDir") <- file.path(cacheRoot, cacheDir)
	filePath
}

.deleteTmpCacheFile <-
		function(object)
{
	unlink(.generateTmpCacheFileName(object))
}

.deleteCacheFile <-
		function(object)
{
	unlink(.generateCacheFileName(object))
}

.loadCachedObjects <-
		function(envir, cacheRoot = .getCache("synapseCacheDir"), cacheDir = .getCache("rObjCacheDir"))
{
	lapply(
			list.files(file.path(cacheRoot, cacheDir), full.names=T, pattern = "rbin$"),
			FUN = function(filepath){
				load(filepath, envir = envir)
			}
	)
}