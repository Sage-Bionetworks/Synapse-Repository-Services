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
			code <- Layer(entity=entity)
			
			## coerce to Code
			class(code) <- "Code"
			code <- initialize(code)
			synapseEntityKind(code) <- synapseEntityKind(new(Class="Code"))
			code
		}
)

setMethod(
		f = "Code",
		signature = "missing",
		definition = function(entity){
			Code(list())
		}
)

setMethod(
		f = "initialize",
		signature = "Code",
		definition = function(.Object, ...){
			annotValue(.Object, "format") <- "code"
			#propertyValue(.Object, "type") <- "M"
			.Object
		}
)
