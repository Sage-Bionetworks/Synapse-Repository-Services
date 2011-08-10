# Generic method definitions
# 
# Author: matt furia
###############################################################################

setGeneric(name="as.list")

setGeneric(
		name = "refreshEntity",
		def = function(entity){
			standardGeneric("refreshEntity")
		}
)

setGeneric(
		name = "refreshAnnotations",
		def = function(entity){
			standardGeneric("refreshAnnotations")
		}
)

setGeneric(
		name = "updateEntity",
		def = function(entity, ...){
			standardGeneric("updateEntity")
		}
)

setGeneric(
		name = "createEntity",
		def = function(entity, ...){
			standardGeneric("createEntity")
		}
)

setGeneric(
		name = "getEntity",
		def = function(entity, ...){
			standardGeneric("getEntity")
		}
)

setGeneric(
		name = "deleteProperty",
		def = function(object, which){
			standardGeneric("deleteProperty")
		}
)

setGeneric(
		name = "deleteAnnotation",
		def = function(object, which){
			standardGeneric("deleteAnnotation")
		}
)

setGeneric(
		name = "deleteAnnotations",
		def = function(object){
			standardGeneric("deleteAnnotations")
		}
)

setGeneric(
		name = "deleteEntity",
		def = function(entity, ...){
			standardGeneric("deleteEntity")
		}
)

setGeneric(
		name = ".extractEntityFromSlots",
		def = function(object){
			standardGeneric(".extractEntityFromSlots")
		}
)

setGeneric(
		name = ".populateSlotsFromEntity",
		def = function(object, entity){
			standardGeneric(".populateSlotsFromEntity")
		}
)

setGeneric(
		name = "loadLayerData",
		def = function(entity){
			standardGeneric("loadLayerData")
		}
)

setGeneric(
		name = "storeLayerData",
		def = function(entity, ...){
			standardGeneric("storeLayerData")
		}
)

setGeneric(
		name = "storeLayerDataFile",
		def = function(entity, layerDataFilepath){
			standardGeneric("storeLayerDataFile")
		}
)
setGeneric(
		name = "storeLayerDataFiles",
		def = function(entity, layerDataFile){
			standardGeneric("storeLayerDataFiles")
		}
)

setGeneric(
		name = "updateAnnotations",
		def = function(annotations){
			standardGeneric("updateAnnotations")
		}
)

setGeneric(
		name="annotations<-", 
		def=function(object,value){
			standardGeneric("annotations<-")
		}
)

setGeneric(
		name = "annotations",
		def = function(object){
			standardGeneric("annotations")
		}
)

setGeneric(
		name = "properties",
		def = function(object){
			standardGeneric("properties")
		}
)

setGeneric(
		name = "properties<-",
		def = function(object, ...){
			standardGeneric("properties<-")
		}
)

setGeneric(
		name = "deleteProperty",
		def = function(object, which){
			standardGeneric("deleteProperty")
		}
)

setGeneric(
		name = "annotValue",
		def = function(object, which){
			standardGeneric("annotValue")
		}
)

setGeneric(
		name = "annotValue<-",
		def = function(object, which, value, ...){
			standardGeneric("annotValue<-")
		}
)

setGeneric(
		name = "propertyValue",
		def = function(object, which){
			standardGeneric("propertyValue")
		}		
)

setGeneric(
		name = "propertyValue<-",
		def = function(object, which, ...){
			standardGeneric("propertyValue<-")
		}		
)

setGeneric(
		name = ".setAnnotationValue",
		def = function(object, which, value, type){
			standardGeneric(".setAnnotationValue")
		}
)

setGeneric(
		name = "annotationNames",
		def = function(object){
			standardGeneric("annotationNames")
		}
)

setGeneric(
		name = "annotationValues",
		def = function(object){
			standardGeneric("annotationValues")
		}
)

setGeneric(
		name = "annotationValues<-",
		def = function(object, values){
			standardGeneric("annotationValues<-")
		}
)

setGeneric(
		name = "propertyNames",
		def = function(object){
			standardGeneric("propertyNames")
		}
)

setGeneric(
		name = "propertyValues",
		def = function(object){
			standardGeneric("propertyValues")
		}
)

setGeneric(
		name = "propertyValues<-",
		def = function(object, values){
			standardGeneric("propertyValues<-")
		}
)

## Generic method for setting subclass
setGeneric(
		name = "setSubclass",
		def = function(object){
			standardGeneric("setSubclass")
		}
)

## Generic Location constructor
setGeneric(
		name = "Location",
		def = function(entity){
			standardGeneric("Location")
		}
)

## Generic Dataset constructor
setGeneric(
		name = "Dataset",
		def = function(entity){
			standardGeneric("Dataset")	
		}
)

## Generic SynapseEntity constructor
setGeneric(
		name = "SynapseEntity",
		def = function(entity, ...){
			standardGeneric("SynapseEntity")
		}
)

## Generic Layer constructor
setGeneric(
		name = "Layer",
		def = function(entity){
			standardGeneric("Layer")
		}
)

## Generic Project constructor
setGeneric(
		name = "Project",
		def = function(entity){
			standardGeneric("Project")
		}
)

setGeneric(
		name = "synapseEntityKind",
		def = function(entity){
			standardGeneric("synapseEntityKind")
		}
)

setGeneric(
		name = "synapseEntityKind<-",
		def = function(entity, ...){
			standardGeneric("synapseEntityKind<-")
		}
)


