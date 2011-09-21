# Functions for manipulating the object cache 
# 
# Author: Matt Furia
###############################################################################


.cacheObject <-
		function(entity, objectName)
{
	destFile <- .generateCacheFileName(entity, objectName)
	if(!file.exists(attr(destFile, "cacheDir")))
		dir.create(attr(destFile, "cacheDir"), recursive = TRUE)

	save(list = objectName, envir = entity@objects, file = destFile)
}

.tmpCacheObject <- 
		function(entity, objectName)
{
	if(!file.exists(.generateCacheFileName(entity, objectName)))
		stop("source file does not exist")
	file.rename(.generateCacheFileName(entity, objectName), .generateTmpCacheFileName(entity, objectName))
}

.renameCacheObjectFromTmp <-
		function(entity, srcName, destName)
{
	file.rename(.generateTmpCacheFileName(entity, srcName), .generateCacheFileName(entity, destName))
}

.generateCacheFileName <- 
		function(entity, objectName, cacheSubDir = .getCache("rObjCacheDir"))
{
	kSuffix <- "rbin"
	filePath <- file.path(entity$cacheDir, cacheSubDir, sprintf("%s.%s", objectName, kSuffix))
	attr(filePath, "cacheDir") <- file.path(entity$cacheDir, cacheSubDir)
	filePath
}

.generateTmpCacheFileName <- 
		function(entity, objectName, cacheSubDir = .getCache("rObjCacheDir"))
{
	kSuffix <- "rbin.tmp"
	filePath <- file.path(entity$cacheDir, cacheSubDir, sprintf("%s.%s", objectName, kSuffix))
	attr(filePath, "cacheDir") <- file.path(entity$cacheDir, cacheSubDir)
	filePath
}

.deleteTmpCacheFile <-
		function(entity, objectName)
{
	unlink(.generateTmpCacheFileName(entity, objectName))
}

.deleteCacheFile <-
		function(entity, objectName)
{
	unlink(.generateCacheFileName(entity, objectName))
}

.loadCachedObjects <-
		function(entity, cacheSubDir = .getCache("rObjCacheDir"))
{
	lapply(
			list.files(file.path(entity$cacheDir, cacheSubDir), full.names=T, pattern = "rbin$"),
			FUN = function(filepath){
				load(filepath, envir = entity@objects)
			}
	)
	invisible(entity)
}