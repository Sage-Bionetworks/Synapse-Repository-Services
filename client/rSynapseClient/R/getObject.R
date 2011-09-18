# Get Objects from a Layer Entity
# 
# Author: Matt Furia
###############################################################################


setGeneric(
		name = "getObject",
		def = function(entity, which){
			standardGeneric("getObject")
		}
)

setMethod(
		f = "getObject",
		signature = signature("Layer", "character"),
		definition = function(entity, which){
			get(which, envir = entity@objects)
		}
)