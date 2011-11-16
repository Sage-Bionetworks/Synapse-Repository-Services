# Methods for storing Entity objects
# 
# Author: Matt Furia
###############################################################################

setGeneric(
		name = "storeEntityObjects",
		def = function(entity){
			standardGeneric("storeEntityObjects")
		}
)

setMethod(
		f = "storeEntityObjects",
		signature = "Layer",
		def = function(entity){
			
			## get the entity files
			files <- list.files(file.path(entity$cacheDir, .getCache("rObjCacheDir")), all.files = TRUE, full.names=TRUE)
			files <- intersect(files, .generateCacheFileName(entity, names(entity$objects)))
			if(length(files) == 0)
				stop("Entity contains no objects that can be stored")
			files <- c(entity@location@files,files)
			files <- gsub(entity$cacheDir, "", files, fixed = TRUE)
			if(!all(file.exists(file.path(entity$cacheDir, files))))
				stop("Not all files listed by the entity exist.")
			
			## build the outfile name
			dataFileName <- gsub("^[\\/]+", "", tempfile(fileext=".zip", tmpdir=""))
			if(!is.null(propertyValue(entity, "name")))
				dataFileName <- sprintf("%s%s",gsub("[ ]+", "_", propertyValue(entity, "name")),".zip")
			
			dataFileName <- file.path(tempdir(), dataFileName)
			
			## if zipFile exists delete it before creating
			if(file.exists(dataFileName))
				file.remove(dataFileName)
			
			
			## change directory to the cache directory
			oldDir <- getwd()
			setwd(entity@location@cacheDir)
			suppressWarnings(zipRetVal <- zip(zipfile=normalizePath(dataFileName, mustWork=FALSE), files=gsub("^/","",files)))
			setwd(oldDir)
			
			## if zip fails, load uncompressed
			if(zipRetVal != 0L){
				msg <- sprintf("Unable to zip layerData Files. Error code: %i.",zipRetVal)
				if(length(files) > 1)
					stop(msg, " Make sure that zip is installed on your computer. Without zip, only one file can be uploaded at a time")
				warning("Zip was not installed on your computer. Uploading layer data uncompressed. Directory structure will not be preserved.")
				dataFileName <- gsub("^/","",files)
			}
			
			storeFile(entity, dataFileName)
		}
)

