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

setMethod(
		f = "getParentEntity",
		signature = "numeric",
		definition = function(entity){
			getParentEntity(as.character(entity))
		}
)

setMethod(
		f = "getParentEntity",
		signature = "character",
		definition = function(entity){
			entity <- getEntity(entity)
			getEntity(propertyValue(entity, "id"))
		}
)
