
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
			if(!is.null(propertyValue(layer, "id")))
				layer@synapseWebUrl <- .buildSynapseUrl(propertyValue(layer, "id"))
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

setMethod(
		f = "Layer",
		signature = "missing",
		definition = function(entity){
			Layer(list())
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
			## TODO: this is a bit of  a kluge. need to redesign
			## the way subclases are determined
			if(!is.null(layerType <- propertyValue(object, "type"))){
				if(layerType == "M"){
					format <- annotValue(object, "format")
					if(!is.null(format) && tolower(format) == "code")
						layerType <- "Code"
				}
				if(!is.null(subClassType <- .getCache("layerCodeTypeMap")[[layerType]]))
					layerType <- subClassType
			}
			
			
			
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
			refreshedEntity@objects <- entity@objects
			refreshedEntity
		}
)

setMethod(
		f = "show",
		signature = "Layer",
		definition = function(object){
			cat('An object of class "', class(object), '"\n', sep="")
			
			cat("Synapse Entity Name : ", properties(object)$name, "\n", sep="")
			cat("Synapse Entity Id   : ", properties(object)$id, "\n", sep="")
			
			if (!is.null(properties(object)$parentId))
				cat("Parent Id           : ", properties(object)$parentId, "\n", sep="")
			if (!is.null(properties(object)$type))
				cat("Type                : ", properties(object)$type, "\n", sep="")
			if (!is.null(properties(object)$version))
				cat("Version             : ", properties(object)$version, "\n", sep="")
			
			obj.msg <- summarizeObjects(object)
			if(!is.null(obj.msg)){
				cat("\n", obj.msg$count,":\n", sep="")
				cat(obj.msg$objects, sep="\n")
			}
			
			files.msg <- summarizeCacheFiles(object@location)
			if(!is.null(files.msg))
				cat("\n", files.msg$count, "\n", sep="")
			if(!is.null(propertyValue(object,"id"))){
				cat("\nFor complete list of annotations, please use the annotations() function.\n")
				cat(sprintf("To view this Entity on the Synapse website use the 'onWeb()' function\nor paste this url into your browser: %s\n", object@synapseWebUrl))
			}
		}
)

setMethod(
		f = "summarizeObjects",
		signature = "Layer",
		definition = function(entity){
			msg <- NULL
			if(length(objects(entity@objects)) > 0){
				msg$count <- sprintf("loaded object(s)")
				objects <- objects(entity@objects)
				classes <- unlist(lapply(objects, function(object){class(entity@objects[[object]])}))
				
				msg$objects <- sprintf('[%d] "%s" (%s)', 1:length(objects), objects, classes)
			}
			msg
		}
)

setMethod(
		f = "[",
		signature = "Layer",
		definition = function(x, i, j, ...){
			if(length(as.character(as.list(substitute(list(...)))[-1L])) > 0L || !missing(j))
				stop("incorrect number of subscripts")
			if(is.numeric(i)){
				if(any(i > length(names(x))))
					stop("subscript out of bounds")
				i <- names(x)[i]
			}else if(is.character(i)){
				if(!all(i %in% names(x)))
					stop("undefined objects selected")
			}else{
				stop(sprintf("invalid subscript type '%s'", class(i)))
			}
			retVal <- lapply(i, function(i){
						if(i=="objects"){
							envir <- slot(x,i)
							objects <- lapply(objects(envir), function(key) get(key,envir=envir))
							names(objects) <- objects(envir)
							return(objects)
						}
						slot(x@location, i)
					}
			)
			names(retVal) <- i
			retVal
		}
)

setMethod(
		f = "[[",
		signature = "Layer",
		definition = function(x, i, j, ...){
			if(length(as.character(as.list(substitute(list(...)))[-1L])) > 0L || !missing(j))
				stop("incorrect number of subscripts")
			if(length(i) > 1)
				stop("subscript out of bounds")
			x[i][[1]]
		}
)
setMethod(
		f = "$",
		signature = "Layer",
		definition = function(x, name){
			x[[name]]
		}
)

#setMethod(
#		f = "$<-",
#		signature = "Layer",
#		definition = function(x, name, value){
#			cat(name,"\n")
#		}
#)


setMethod(
		f = "names",
		signature = "Layer",
		definition = function(x){
			c("objects", "cacheDir", "files")
		}
)

setMethod(
		f = "initialize",
		signature = "Layer",
		definition = function(.Object, properties=NULL){
			.Object@objects <- new.env(parent=emptyenv())
			if(!is.null(properties))
				.Object@properties <- properties
			.Object@location = new(Class="CachedLocation")
			.Object
		}
)
