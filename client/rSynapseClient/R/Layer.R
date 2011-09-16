
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
			layer <- new(Class = "Layer")
			layer <- .populateSlotsFromEntity(layer, entity)
			
			## add annotations
			annotations <- new(Class="SynapseAnnotation")
			if(!is.null(entity$id)){
				annotations <- tryCatch(
						SynapseAnnotation(getAnnotations(entity=entity)),
						error = function(e){
							warning("Unable to retrieve annotations for entity.")
						}
				)
			}
			layer@annotations<- annotations
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

setMethod(
		f = "refreshEntity",
		signature = "Layer",
		definition = function(entity){
			refreshedEntity <- getEntity(entity)
			refreshedEntity@location <- entity@location
			refreshedEntity@loadedObjects <- entity@loadedObjects
			refreshedEntity
		}
)

setMethod(
		f = "initialize",
		signature = signature("Layer"),
		definition = function(.Object, properties=NULL){
			.Object@loadedObjects <- new.env(parent=emptyenv())
			if(!is.null(properties))
				.Object@properties <- properties
			.Object
		}
)

