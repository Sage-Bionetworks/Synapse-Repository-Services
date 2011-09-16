# TODO: Add comment
# 
# Author: Matt Furia
###############################################################################


setMethod(
		f = "copyEntity",
		signature = "Layer",
		definition = function(entity){
			copy <- entity
			copy@loadedObjects <- new.env()
			for(key in objects(entity@loadedObjects))
				assign(key, get(key,envir=entity@loadedObjects), envir=copy@loadedObjects)
			copy
		}
)
