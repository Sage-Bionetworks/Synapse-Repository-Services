### class definitions
#
setClass(Class="layerList")
setClass(Class="layerFiles")

setClass(
		Class = "Layer",
		representation(
				id = "character",
				parentId = "character",
				status = "character",
				numSamples = "numeric",
				platform = "character",
				locations = "character",
				version = "character",
				description = "character",
				processingFacility = "character",
				creationDate = "character",
				publicationDate = "character",
				qcDate = "character",
				uri = "character",
				name = "character",
				previews = "character",
				cachedFiles = "layerFiles"
		),
		prototype = NULL
)

setClass(
		Class = "MicroArrayLayer",
		representation(
				vendor = "character",
				platform = "character"
		),
		contains = "Layer",
		prototype = NULL
)

setClass(
		Class = "ExpressionLayer",
		representation = representation(
				tissueType = "character"
		),
		contains = "MicroArrayLayer",
		prototype = prototype(
				id = NULL,
				status = NULL,
				tissueType = NULL,
				vendor = NULL,
				platform = NULL
		)
)

setClass(
		Class = "GenotypeLayer",
		representation = representation(
				
				
		),
		contains = "MicroArrayLayer",
		prototype = prototype(
				id = NULL,
				status = NULL,
				vendor = NULL,
				platform = NULL
		)
)

setClass(
		Class = "PhenotypeLayer",
		contains = "Layer",
		prototype = prototype(
				datasetId = NULL,
				status = NULL,
				numRecords = 0
		)
)
#####
### Constructors for the various Layer types
#####

Layer <- function(object, ...){
	
}

setGeneric(
	name = "Layer",
	def = function(object, object2, ...){
		standardGeneric("Layer")
	},
	valueClass = "Layer"
)


setGeneric(
	name = "layerType",
	def = function(object){
		standardGeneric("layerType")
	}
)

setMethod(
		f = "Layer",
		signature = "layerList",
		definition = function(object){
				map <- .getCache("layerCodeTypeMap")
				ind <- which(.getCache("layerCodeTypeMap") == object$type)
				if(length(ind) == 1){
					layerClass <- names(map)[ind]
					Layer(new(Class = layerClass), object)
				}
		}	
)

setMethod(
		f = "Layer",
		signature = c("PhenotypeLayer", "layerList"),
		definition = function(object, object2){
			object@id <- object2$id
			object@numSamples <- object2$numSamples
			object@status <- as.character(object2$status)
			object@processingFacility <- as.character(object2$processingFacility)
			object@publicationDate <- as.character(object2$publicationDate)
			object@version <- as.character(object2$version)
			object@creationDate <- as.character(object2$creationDate)
			object@uri <- as.character(object2$uri)
			object@locations <- as.character(object2$locations)
			object@parentId <- as.character(object2$parentId)
			object@description <- as.character(object2$description)
			object@name <- as.character(object2$name)
			object@previews <- as.character(object2$previews)
			return(object)
		}
)

setMethod(
		f = "Layer",
		signature = c("ExpressionLayer", "layerList"),
		definition = function(object, object2){
			object@id <- object2$id
			object@numSamples <- object2$numSamples
			object@status <- as.character(object2$status)
			object@processingFacility <- as.character(object2$processingFacility)
			object@publicationDate <- as.character(object2$publicationDate)
			object@version <- as.character(object2$version)
			object@creationDate <- as.character(object2$creationDate)
			object@uri <- as.character(object2$uri)
			object@locations <- as.character(object2$locations)
			object@parentId <- as.character(object2$parentId)
			object@description <- as.character(object2$description)
			object@name <- as.character(object2$name)
			object@previews <- as.character(object2$previews)
			return(object)
		}
)

setMethod(
		f = "Layer",
		signature = c("GenotypeLayer", "layerList"),
		definition = function(object, object2){
			object@id <- object2$id
			object@numSamples <- object2$numSamples
			object@status <- as.character(object2$status)
			object@processingFacility <- as.character(object2$processingFacility)
			object@publicationDate <- as.character(object2$publicationDate)
			object@version <- as.character(object2$version)
			object@creationDate <- as.character(object2$creationDate)
			object@uri <- as.character(object2$uri)
			object@locations <- as.character(object2$locations)
			object@parentId <- as.character(object2$parentId)
			object@description <- as.character(object2$description)
			object@name <- as.character(object2$name)
			object@previews <- as.character(object2$previews)
			return(object)
		}
)


setGeneric(
		name = "getLocations",
		def = function(object){
			standardGeneric("getLocations")
		}
)

setMethod(
		f = "getLocations",
		signature = "Layer",
		definition = function(object){
			return(synapseGet(object@locations))
		}
)

setMethod(
		f = "layerType",
		signature = "Layer",
		definition = function(object){
			return(.getCache("layerCodeTypeMap")[[as.character(class(object))]])
		}
)

setMethod(
		f = "show",
		signature = "ExpressionLayer",
		definition = function(object){
			cat(as.character(class(object)), 
					"\nlayer type: ",layerType(object),
					"\n\tstatus: ", object@status,
					"\n\tlayerId: ", object@id,
					"\n\tplatform: ", object@platform,
					"\n\tnumSamples: ", object@numSamples,
					"\n\ttissue: ", object@tissueType,
					"\n"
			)
			if(!is.null(object@cachedFiles)){
				cat("\n\tCachedFiles:\n")
				cat(paste("\t",paste(pt@cachedFiles, collapse="\n\t"), sep=""),"\n")
			}
		}
)

setMethod(
		f = "show",
		signature = "Layer",
		definition = function(object){
			
			cat(as.character(class(object)),
					"\nlayer type: ", layerType(object),
					"\n\tlayerId: ", object@id,
					"\n\tstatus: ", object@status,
					"\n\tnumSamples: ", object@numSamples
			)
			if(!is.null(object@cachedFiles)){
				cat("\n\tCachedFiles:\n")
				cat(paste("\t",paste(pt@cachedFiles, collapse="\n\t"), sep=""),"\n")
			}
		}
)

setMethod(
		f = "show",
		signature = "MicroArrayLayer",
		definition = function(object){
			cat(as.character(class(object)), 
					"\nlayer type: ",layerType(object),
					"\n\tstatus: ", object@status,
					"\n\tlayerId: ", object@id,
					"\n\tplatform: ", object@platform,
					"\n\tnumSamples: ", object@numSamples,
					"\n"
			)
			if(!is.null(object@cachedFiles)){
				cat("\n\tCachedFiles:\n")
				cat(paste("\t",paste(pt@cachedFiles, collapse="\n\t"), sep=""),"\n")
			}
		}
)

setMethod(
		f = "show",
		signature = "PhenotypeLayer",
		definition = function(object){
			
			cat(as.character(class(object)),
					"\nlayer type: ", layerType(object),
					"\n\tlayerId: ", object@id,
					"\n\tstatus: ", object@status,
					"\n\tnumSamples: ", object@numSamples, 
					"\n"
			)
			if(!is.null(object@cachedFiles)){
				cat("\n\tCachedFiles:\n")
				cat(paste("\t",paste(pt@cachedFiles, collapse="\n\t"), sep=""),"\n")
			}
		}
)