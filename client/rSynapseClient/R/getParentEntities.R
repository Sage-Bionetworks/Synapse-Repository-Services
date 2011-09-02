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
			getEntity(entityId, synapseEntityKind(entity))
		}
)
