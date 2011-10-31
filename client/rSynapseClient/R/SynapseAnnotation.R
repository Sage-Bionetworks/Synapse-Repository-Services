# S4 Classes and methods for accessing and manipulating Annotations
# 
# Author: Matt Furia
###############################################################################

## Constructor
SynapseAnnotation <- 
		function(entity)
{
	if(!is.list(entity))
		stop("entity must be a list.")
	
	annotations <- new(Class = "SynapseAnnotation")
	if(any(names(entity) == ""))
		stop("all elements of the entity must be named")
	.populateSlotsFromEntity(annotations, entity)
}

## show method
setMethod(
		f = "show",
		signature = "SynapseAnnotation",
		definition = function(object){
			cat('An object of class "', class(object), '"\n', sep="")
			lapply(annotationNames(object),FUN=function(n){cat(n,"=",paste(annotValue(object,n),collapse=","),"\n",sep="")})
		}	
)

## extractor for annotation/property names
setMethod(
		f = "annotationNames",
		signature = "SynapseAnnotation",
		definition = function(object){
			kPropertiesSlotName <- "properties"
			## helper function for getting names
			getNames <- function(n){
				if(!is.null(names(slot(object,n))))
					return(names(slot(object,n)))
			}
			unlist(lapply(setdiff(slotNames(object), kPropertiesSlotName), FUN=getNames))
		}
)

setMethod(
		f = "annotationValues",
		signature = "SynapseAnnotation",
		definition = function(object){
			lapply(annotationNames(object),FUN=annotValue, object=object)
		}
)

setMethod(
		f = "annotationValues<-",
		signature = signature("SynapseAnnotation", "list"),
		def = function(object, value){
			if(any(names(value) == ""))
				stop("All entity elements must be named")
			for(name in names(value))
				annotValue(object, name) <- value[[name]]
			object
		}
)
setMethod(
		f = "propertyNames",
		signature = "SynapseAnnotation",
		definition = function(object){
			kPropertiesSlotName <- "properties"
			names(slot(object, kPropertiesSlotName))
		}
)

setMethod(
		f = "propertyValues",
		signature = "SynapseAnnotation",
		definition = function(object){
			lapply(propertyNames(object), FUN=function(n){propertyValue(object,n)})
		}
)

setMethod(
		f = "as.list",
		signature = "SynapseAnnotation",
		definition = function(x, ...){
			vals <- annotationValues(x)
			names(vals) <- annotationNames(x)
			vals
		}
)

setMethod(
		f = ".extractEntityFromSlots",
		signature = "SynapseAnnotation",
		definition = function(object){
			entity <- list()
			for(n in slotNames(object)){
				if(n == "properties"){
					for(nn in names(slot(object,n))){
						entity[[nn]] <- slot(object,n)[[nn]]
					}
				}else{
					entity[[n]] <- slot(object, n)
				}
			}
			entity
		}
)

setMethod(
		f = ".populateSlotsFromEntity",
		signature = signature("SynapseAnnotation", "list"),
		definition = function(object, entity){
			
			## set the slots whose names match the argument names
			indx <- which(names(entity) %in% setdiff(slotNames(object), "properties"))
			for(name in names(entity)[indx]){
				slot(object, name) <- entity[[name]]
			}
			
			## put all the unmatched names into the properties slot
			for(name in names(entity)[-indx]){
				object@properties[name] <- entity[[name]]
			}
			object
		}
)


#####
## Annotation/property value getters
#####

setMethod(
		f = "annotValue",
		signature = signature("SynapseAnnotation", "character"),
		definition = function(object, which){
			kPropertiesSlotName <- "properties"
			nms <- annotationNames(object)
			if(sum(nms == which) == 0L)
				return(NULL)
			if(sum(nms == which) > 1L)
				warning("Multiple values found for annotation. Returning the first occurance")
			
			nms <- setdiff(slotNames(object), kPropertiesSlotName)
			slotName <- nms[sapply(nms, FUN=function(name){which %in% names(slot(object,name))})]
			value <- slot(object,slotName[1])[[which]]
			
#			if(grepl("date", tolower(which)))
#				value <- as.POSIXct(as.integer(value), origin=ISOdatetime(1970,1,1,0,0,0))
			return(value)
			## TODO: return values cast to the correct type
		}
)

setMethod(
		f = "propertyValue",
		signature = signature("SynapseAnnotation", "character"),
		definition = function(object, which){
			value <- properties(object)[[which]]
#			if(grepl("date", tolower(which)))
#				value <- as.POSIXct(value, origin=ISOdatetime(1970,1,1,0,0,0))
			value
		}
)

#####
## End getters
#####

#####
## Annotation/property value setters
#####

setMethod(
		f = ".setAnnotationValue",
		signature = signature("SynapseAnnotation", "character", "character", "character"),
		definition = function(object, which, value, type){
			if(!type %in% slotNames(object))
				stop("unknown annotation type:", type)
			
			## if the annotation name is already stored as a different type, coerce the
			## R type associated with the annotation type and try again
			slots <- setdiff(slotNames(object), c(type, "properties"))
			mask <- sapply(slots, FUN = function(slotName){
						which %in% names(slot(object, slotName))
					}
			)
			if(sum(mask) > 1L)
				stop("illegal state: annotation name can only be present in one slot")
			if(sum(mask) == 1L){
				type <- names(mask)[which(mask)]
				return(.setAnnotationValue(object = object, which = which, value = value, type = type))
			}
			
			## the annotation was either not assigned, or a value was already assigned the implied type
			slot(object, type)[[which]] <- value
			object
		}
)

setMethod(
		f = "annotValue<-",
		signature = signature("SynapseAnnotation", "character", "character"),
		definition = function(object, which, value){
			if(grepl("date", gsub("update", "", tolower(which))))
				stop("invalid data type for date annotations. See the documentation for 'POSIXct' or 'Date' for details on how to construct the correct value for date annotations.")
			type <- names(which(.getCache("annotationTypeMap") == class(value)))
			.setAnnotationValue(object = object, which = which, value = value, type = type)
		}
)

setMethod(
		f = "annotValue<-",
		signature = signature("SynapseAnnotation", "character", "numeric"),
		definition = function(object, which, value){
			if(grepl("date", tolower(which)))
				stop("invalid data type for date annotations. See the documentation for 'POSIXct' or 'Date' for details on how to construct the correct value for date annotations.")
			type <- names(which(.getCache("annotationTypeMap") == class(value)))
			#all annotations are stored internally as strings
			.setAnnotationValue(object = object, which = which, value = as.character(value), type = type)
		}
)

setMethod(
		f = "annotValue<-",
		signature = signature("SynapseAnnotation", "character", "integer"),
		definition = function(object, which, value){
			if(grepl("date", tolower(which)))
				stop("invalid data type for date annotations. See the documentation for 'POSIXct' or 'Date' for details on how to construct the correct value for date annotations.")
			type <- names(which(.getCache("annotationTypeMap") == class(value)))
			#all annotations are stored internally as strings
			.setAnnotationValue(object = object, which = which, value = as.character(value), type = type)
		}
)

#setMethod(
#		f = "annotValue<-",
#		signature = signature("SynapseAnnotation", "character", "POSIXt"),
#		definition = function(object, which, value){
#			if(!grepl("date", tolower(which)))
#				stop("Annotations with date values must include the string 'date' in the annotation name.")
#			map <- .getCache("annotationTypeMap")
#			type <- names(map)[which(map %in% class(value))] ##POSIX dates return 2 class types
#			#all annotations are stored internally as strings
#			value <- as.character(as.integer(value))
#			.setAnnotationValue(object = object, which = which, value = value, type = type)
#		}
#)

#setMethod(
#		f = "annotValue<-",
#		signature = signature("SynapseAnnotation", "character", "Date"),
#		definition = function(object, which, value){
#			annotValue(object = object, which = which) <- as.POSIXct(value)
#			object
#		}
#)

setMethod(
		f = "annotValue<-",
		signature = signature("SynapseAnnotation", "character", "logical"),
		definition = function(object, which, value){
			annotValue(object = object, which = which) <- as.character(value)
			object
		}
)

setMethod(
		f = "propertyValue<-",
		signature = signature("SynapseAnnotation", "character"),
		definition = function(object, which, value){
			properties(object)[[which]] <- value
			object
		}
)
setMethod(
		f = "propertyValue<-",
		signature = signature("SynapseAnnotation", "numeric"),
		definition = function(object, which, value){
			properties(object)[[which]] <- value
			object
		}
)
setMethod(
		f = "propertyValue<-",
		signature = signature("SynapseAnnotation", "integer"),
		definition = function(object, which, value){
			properties(object)[[which]] <- value
			object
		}
)
setMethod(
		f = "propertyValue<-",
		signature = signature("SynapseAnnotation", "logical"),
		definition = function(object, which, value){
			properties(object)[[which]] <- value
			object
		}
)
#setMethod(
#		f = "propertyValue<-",
#		signature = signature("SynapseAnnotation", "Date"),
#		definition = function(object, which, value){
#			properties(object)[[which]] <- value
#			object
#		}
#)
#
#setMethod(
#		f = "propertyValue<-",
#		signature = signature("SynapseAnnotation", "POSIXt"),
#		definition = function(object, which, value){
#			if(!grepl("date", tolower(which)))
#				stop("date valued properties must contain 'date' in the name")
#			properties(object)[[which]] <- as.integer(value)
#			object
#		}
#)

setMethod(
		f = "propertyValues<-",
		signature = signature("SynapseAnnotation", "list"),
		definition = function(object, value){
			if(any(names(value) == ""))
				stop("all list elements must be named")
			for(key in propertyNames(object)){
				propertyValue(object, key) <- value[key]
			}
			object
		}
)

setMethod(
		f = "deleteProperty",
		signature = signature("SynapseAnnotation", "character"),
		definition = function(object, which){
			if(!all(indx <- (which %in% propertyNames(object))))
				warning(paste(which[-indx], sep="", collapse=","), "were not valid properties, so were not deleted.")
			object@properties <- object@properties[setdiff(propertyNames(object), which)]
			object
		}
)

setMethod(
		f = "deleteAnnotation",
		signature = signature("SynapseAnnotation", "character"),
		definition = function(object, which){
			if(!all(indx <- (which %in% annotationNames(object))))
				warning(paste(which[-indx], sep="", collapse=","), "were not valid annotations, so were not deleted.")
			for(thisWhich in which){
				type <- setdiff(slotNames(object),"properties")[unlist(lapply(setdiff(slotNames(object),"properties"), FUN=function(sn){thisWhich %in% names(slot(object,sn))}))]
				for(thisType in type){
				slot(object,thisType) <- slot(object, thisType)[setdiff(names(slot(object,thisType)), thisWhich)]
				}
			}
			object
		}
)

#####
## End setters
#####

setMethod(
		f = "properties",
		signature = "SynapseAnnotation",
		definition = function(object){
			kPropertiesSlotName <- "properties"
			slot(object, kPropertiesSlotName)
		}
)

setMethod(
		f = "properties<-",
		signature = signature("SynapseAnnotation"),
		definition = function(object, value){
			object@properties <- value
			object
		}
)


