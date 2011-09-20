# delete object from Layer entity
# 
# Author: Matt Furia
###############################################################################

setGeneric(
		name = "deleteObject",
		def = function(entity, which){
			standardGeneric("deleteObject")
		}
)

setMethod(
		f = "deleteObject",
		signature = signature("Layer", "character"),
		definition = function(entity, which){
			rm(list=which, envir=entity@objects)
			tryCatch(
					.deleteCacheFile(which),
					error = function(e){
						warning(sprintf("Unable to delete cache file associated with %s\n%s", which, e))
					},
					warning = function(e){
						warning(sprintf("Unable to delete cache file associated with %s\n%s", which, e))
					}
			)
			invisible(entity)
		}
)