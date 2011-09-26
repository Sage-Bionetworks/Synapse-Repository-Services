# TODO: Add comment
# 
# Author: furia
###############################################################################


setMethod(
		f = "Code",
		signature = "character",
		definition = function(entity){
			getEntity(entity)
		}
)

setMethod(
		f = "Code",
		signature = "numeric",
		definition = function(entity){
			Code(as.character(entity))
		}
)

setMethod(
		f = "Code",
		signature = "list",
		definition = function(entity){
			code <- SynapseEntity(entity=entity)
			
			## coerce to Code
			class(code) <- "Code"
			synapseEntityKind(code) <- synapseEntityKind(new(Class="Code"))
			code
		}
)
