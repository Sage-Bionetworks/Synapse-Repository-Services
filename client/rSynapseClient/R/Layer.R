
#####
### Layer Class definitions
#####
setClass(
		Class = "Layer",
		contains = "SynapseEntity",
		prototype = prototype(
				synapseEntityKind = "layer"
		)
)

setClass(
		Class = "ExpressionLayer",
		contains = "Layer"
)

setClass(
		Class = "AffyExpressionLayer",
		contains = "Layer"
)

setClass(
		Class = "AgilentExpressionLayer",
		contains = "Layer"
)

setClass(
		Class = "IlluminaExpressionLayer",
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
PhenotypeLayer <- function(entity){
	entity <- Layer(entity)
	class(entity) <- "PhenotypeLayer"
	return(entity)	
}
IlluminaExpressionLayer <- function(entity){
	entity <- Layer(entity)
	class(entity) <- "IlluminaExpressionLayer"
			return(entity)	
}
GenotypeLayer <- function(entity){
	entity <- Layer(entity)
	class(entity) <- "GenotypeLayer"
			return(entity)	
}
AgilentExpressionLayer <- function(entity){
	entity <- Layer(entity)
	class(entity) <- "AgilentExpressionLayer"
			return(entity)	
}
AffyExpressionLayer <- function(entity){
	entity <- Layer(entity)
	class(entity) <- "AffyExpressionLayer"
			return(entity)	
}
ExpressionLayer <- function(entity){
	entity <- Layer(entity)
	class(entity) <- "ExpressionLayer"
			return(entity)	
}
#####
## Layer constructors
#####
setMethod(
		f = "Layer",
		signature = "list",
		definition = function(entity){

			## call super class constructor
			layer <- SynapseEntity(entity = entity)
			
			## coerce to Layer type. 
			class(layer) <- "Layer"
			synapseEntityKind(layer) <- synapseEntityKind(new(Class="Layer"))

			## first check for subclass
			setSubclass(layer)
		}
)

setMethod(
		f = "Layer",
		signature = "character",
		definition = function(entity){
			entity <- getLayer(entity = entity)
			Layer(entity)
		}
)

setMethod(
		f = "Layer",
		signature = "numeric",
		definition = function(entity){
			as.character(entity)
		}
)

####
## Method for setting the Layer subtype. Uses the annotation type to set the subclass
####
setMethod(
		f = "setSubclass",
		signature = "Layer",
		definition = function(object){
			
			## determine the Layer type and subtype class
			if(!is.null(layerType <- annotValue(object, "type")))
				if(!is.null(subClassType <- .getCache("layerCodeTypeMap")[[layerType]]))
					layerType <- subClassType
			
			## coerce to the correct subclass
			if(!is.null(layerType))
				class(object) <- layerType
			synapseEntityKind(object) <- synapseEntityKind(new(Class="Layer"))
			return(object)
		}
)
