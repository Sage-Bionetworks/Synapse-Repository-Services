setClass(
		Class = "Layer",
		representation(
				annotations = "list",
				cachedFiles = "character"
		),
		prototype = prototype(
				annotations = list(),
				cachedFiles = character()
		)
)

setClass(
		Class = "ExpressionLayer",
		representation = representation(
				tissueType = "character"
		),
		contains = "Layer"
)

setClass(
		Class = "GenotypeLayer",
		contains = "Layer"
)

setClass(
		Class = "PhenotypeLayer",
		contains = "Layer"
)
#####
### Constructors for the various Layer types
#####

#setGeneric(
#	name = "layerType",
#	def = function(object){
#		standardGeneric("layerType")
#	}
#)

Layer <- 
		function(annotations)
{
				map <- .getCache("layerCodeTypeMap")
				ind <- which(.getCache("layerCodeTypeMap") == annotations$type)
				if(length(ind) == 1){
					layerClass <- names(map)[ind]
					layer <- new(Class = layerClass)
					layer@annotations <- annotations
					return(layer)
				}
}	


setGeneric(
		name = "getLocations",
		def = function(object){
			standardGeneric("getLocations")
		}
)

setMethod(
		f = "getLocations",
		signature = "character",
		definition = function(object){
			return(synapseGet(object))
		}
		
)

setMethod(
		f = "getLocations",
		signature = "Layer",
		definition = function(object){
			return(synapseGet(annotations(object)$locations))
		}
)

#setMethod(
#		f = "layerType",
#		signature = "Layer",
#		definition = function(object){
#			return(.getCache("layerCodeTypeMap")[[as.character(class(object))]])
#		}
#)

setGeneric(
		name = "annotations",
		def = function(object){
			standardGeneric("annotations")
			}
)

setMethod(
	f = "annotations",
	signature = "Layer",
	definition = function(object){
		return(object@annotations)
	}
)

setGeneric(
		name="annotations<-", 
		def=function(object,value){
			standardGeneric("annotations<-")
		}
)

setMethod(
		f = "annotations<-",
		signature = "Layer",
		definition = function(object, value){
			object@annotations <- value
			return(object)
		}
)

setMethod(
	f = "show",
	signature = "Layer",
	definition = function(object){
		for(slotName in slotNames(object)){
			cat("@", slotName, "\n", sep="")
			print(slot(object, slotName))
			cat("\n")
		}
	}	
)


setReplaceMethod(
		f = "annotations",
		signature(object="Layer", value="list"),
		function(object, value){
			object@annotations <- value
			return(object)
		}
)






