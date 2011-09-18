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
			invisible(entity)
		}
)