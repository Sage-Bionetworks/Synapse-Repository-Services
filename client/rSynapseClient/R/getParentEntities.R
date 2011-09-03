# Method for getting parent entity
# 
# Author: matt furia
###############################################################################

setMethod(
		f = "getParentEntity",
		signature = "SynapseEntity",
		definition = function(entity){
			if(is.null(entityId <- propertyValue(entity, "parentId")))
				return(NULL)
			tryCatch(
				parent <<- getEntity(entityId),
				error = function(e){
					warning(as.character(e))
					parent <<- NULL
				}
			)
			parent
		}
)
