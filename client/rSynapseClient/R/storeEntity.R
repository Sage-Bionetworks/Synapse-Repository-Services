# TODO: Add comment
# 
# Author: Matt Furia
###############################################################################

setGeneric(
		name = "storeEntity",
		def = function(entity){
			standardGeneric("storeEntity")
		}
)

setMethod(
		f = "storeEntity",
		signature = "Layer",
		definition = function(entity){
			if(length(list.files(file.path(entity$cacheDir, .getCache("rObjCacheDir")), all.files = TRUE)) > 0 ){
				storeEntityObjects(entity)
			} else if(("M" == propertyValue(entity, "type")) && (length(entity$files) == 1) && tolower(class(entity)) != "code") {
				# Special case for media layers, don't zip them
				storeFile(entity, entity$files)
			}
			else {
				storeEntityFiles(entity)
			}
		}
)

setMethod(
		f = "storeEntity",
		signature = "SynapseEntity",
		definition = function(entity){
			stop("StoreEntity is only implemented for Layer entity types")
		}
)