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
		definition = function(entity){
			storeEntityObjects(entity)
		}
)
