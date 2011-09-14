## S4 Class and methods for representing a generic Synapse entity
## 
##  Author: matt furia
###############################################################################

#####
## SynapseEntity "show" method
#####
setMethod(
		f = "show",
		signature = signature("SynapseEntity"),
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
			
			cat("\nFor complete list of annotations, please use the annotations() function.\n")
			cat(sprintf("Or view this Entity on the Synapse website at: %s\n", "https://synapse.sagebase.org"), sep="")
			
			if(length(object@cachedFiles) > 0){
				cat("\nLocal cache directory:\n")
				show(object@cachedFiles$cacheDir)
				cat("\nCached files:\n")
				show(object@cachedFiles$files)
			}
			
			if(length(ls((object@loadedObjects))) > 0){
				cat("\nLoaded objects:\n")
				cat(objects(object@loadedObjects), sep="\n")
			}
				
		}
)

#####
## as.list function. Coerce SynapseEntity to list by returning annotations
#####
setMethod(
		f="as.list",
		signature = "SynapseEntity",
		definition = function(x, ...){
			as.list(annotations(x))			
		}
)

#####
## Get annotation names
#####
setMethod(
		f = "annotationNames",
		signature = "SynapseEntity",
		definition = function(object){
			annotationNames(annotations(object))
		}
)

#####
## Get annotaion values
#####
setMethod(
		f = "annotationValues",
		signature = "SynapseEntity",
		definition = function(object){
			annotationValues(annotations(object))
		}
)

#####
## Set the values for multiple annotations
#####
setMethod(
		f = "annotationValues<-",
		signature = signature("SynapseEntity","list"),
		definition = function(object, value){
			annotationValues(annotations(object)) <- value
			object
		}
)

#####
## getters and setters for SynpaseEntity S4 Class
#####
setMethod(
		f = "annotValue<-",
		signature = signature("SynapseEntity", "character", "character"),
		definition = function(object, which, value){
			if(grepl("date", tolower(which)))
				stop("invalid data type for date annotations. See the documentation for 'POSIXct' or 'Date' for details on how to construct the correct value for date annotations.")
			type <- names(which(.getCache("annotationTypeMap") == class(value)))
			annotations(object) <- .setAnnotationValue(object = annotations(object), which = which, value = value, type = type)
			return(object)
		}
)

setMethod(
		f = "annotValue<-",
		signature = signature("SynapseEntity", "character", "numeric"),
		definition = function(object, which, value){
			if(grepl("date", tolower(which)))
				stop("invalid data type for date annotations. See the documentation for 'POSIXct' or 'Date' for details on how to construct the correct value for date annotations.")
			type <- names(which(.getCache("annotationTypeMap") == class(value)))
			#all annotations are stored internally as strings
			annotations(object) <- .setAnnotationValue(object = annotations(object), which = which, value = as.character(value), type = type)
			return(object)
		}
)

setMethod(
		f = "annotValue<-",
		signature = signature("SynapseEntity", "character", "integer"),
		definition = function(object, which, value){
			if(grepl("date", tolower(which)))
				stop("invalid data type for date annotations. See the documentation for 'POSIXct' or 'Date' for details on how to construct the correct value for date annotations.")
			type <- names(which(.getCache("annotationTypeMap") == class(value)))
			#all annotations are stored internally as strings
			annotations(object) <- .setAnnotationValue(object = annotations(object), which = which, value = as.character(value), type = type)
			return(object)
		}
)

## date setting isn't working correctly. Need to figure out how to translate between Synapse dates (stored as integers)
## and R date classes
#setMethod(
#		f = "annotValue<-",
#		signature = signature("SynapseEntity", "character", "POSIXct"),
#		definition = function(object, which, value){
#			if(!grepl("date", tolower(which)))
#				stop("Annotations with date values must include the string 'date' in the annotation name.")
#			map <- .getCache("annotationTypeMap")
#			type <- names(map)[which(map %in% class(value))] ##POSIX dates return 2 class types
#			#all annotations are stored internally as strings
#			annotations(object) <- .setAnnotationValue(object = annotations(object), which = which, value =  as.character(as.integer(value)), type = type)
#			return(object)
#		}
#)
#
#setMethod(
#		f = "annotValue<-",
#		signature = signature("SynapseEntity", "character", "Date"),
#		definition = function(object, which, value){
#			annotValue(object = annotations(object), which = which) <- as.POSIXct(value)
#			return(object)
#		}
#)

setMethod(
		f = "annotValue<-",
		signature = signature("SynapseEntity", "character", "logical"),
		definition = function(object, which, value){
			annotValue(object = annotations(object), which = which) <- as.character(value)
			return(object)
		}
)

#####
## return the annotations object 
#####
setMethod(
		f = "annotations",
		signature = "SynapseEntity",
		definition = function(object){
			return(object@annotations)
		}
)

#####
## set the annotations object
#####
setMethod(
		f = "annotations<-",
		signature = signature("SynapseEntity","SynapseAnnotation"),
		definition = function(object, value){
			object@annotations <- value
			return(object)
		}
)

#####
## get an annotation value by name
#####
setMethod(
		f = "annotValue",
		signature = signature("SynapseEntity", "character"),
		definition = function(object, which){
			annotValue(annotations(object), which)	
		}
)

#####
## get the list of properties for the object
#####
setMethod(
		f = "properties",
		signature = signature("SynapseEntity"),
		definition = function(object){
		kPropertiesSlotName = "properties"
			slot(object, kPropertiesSlotName)
		}
)

#####
## set the properties list for the object
#####
setMethod(
		f = "properties<-",
		signature = signature("SynapseEntity", "list"),
		definition = function(object, value){
			object@properties <- value
			return(object)
		}
)

#####
## Get the property names
#####
setMethod(
		f = "propertyNames",
		signature = signature("SynapseEntity"),
		definition = function(object){
			kPropertyFieldName <- "properties"
			return(names(slot(object, kPropertyFieldName)))
		}
)

#####
## Get the property values
#####
setMethod(
		f = "propertyValues",
		signature = signature("SynapseEntity"),
		definition = function(object){
			lapply(propertyNames(object),FUN=propertyValue, object=object)
		}
)

#####
## Set multiple property values
#####
setMethod(
		f = "propertyValues<-",
		signature = signature("SynapseEntity", "list"),
		definition = function(object, value){
			if(any(names(value) == ""))
				stop("All entity members must be named")
			for(name in names(value))
				propertyValue(object, name) <- value[[name]]
			return(object)
		}
)

#####annotValue(entity, "dateKey")
## Delete a property
#####
setMethod(
		f = "deleteProperty",
		signature = signature("SynapseEntity", "character"),
		definition = function(object, which){
			if(!all(which %in% propertyNames(object))){
				indx <- which(!(which %in% propertyNames(object)))
				warning(paste(propertyNames(object)[indx], sep="", collapse=","), "were not found in the object, so were not deleted.")
			}
			object@properties <- object@properties[setdiff(propertyNames(object), which)]
			return(object)
		}
)

#####
## Delete an annotation
#####
setMethod(
		f = "deleteAnnotation",
		signature = signature("SynapseEntity", "character"),
		definition = function(object, which){
			annotations(object) <- deleteAnnotation(annotations(object), which)
			return(object)
		}
)

#####
## Get a property value by name
#####
setMethod(
		f = "propertyValue",
		signature = signature("SynapseEntity", "character"),
		definition = function(object, which){
			properties(object)[[which]]
		}
)


#####
## set a property value
#####
setMethod(
		f = "propertyValue<-",
		signature = signature("SynapseEntity", "character"),
		definition = function(object, which, value){
			properties(object)[[which]] <- value
			object
		}
)

#####
## constructor that takes a list entity
#####
setMethod(
		f = "SynapseEntity",
		signature = signature("list"),
		definition = function(entity){
			s4Entity <- new("SynapseEntity")
			s4Entity <- .populateSlotsFromEntity(s4Entity, entity)
			
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
			s4Entity@annotations<- annotations
			s4Entity
		}
)

#####
## convert the S4 entity to a list entity
#####
setMethod(
		f = ".extractEntityFromSlots",
		signature = "SynapseEntity",
		definition = function(object){
			entity <- list()
			for(n in slotNames(object)){
				if(n == "properties"){
					for(nn in names(slot(object,n))){
						entity[[nn]] <- slot(object,n)[[nn]]
					}
				}
			}
			return(entity)
		}
)

#####
## convert the list entity to an S4 entity
#####
setMethod(
		f = ".populateSlotsFromEntity",
		signature = signature("SynapseEntity", "list"),
		definition = function(object, entity){
			if(any(names(entity) == ""))
				stop("All elements of the entity must be named")
			
			## all entity fields should be stored as properties
			for(name in names(entity))
				object@properties[[name]] <- entity[[name]]
			object
		}
)

#####
## Get the Synapse entity kind
#####
setMethod(
		f = "synapseEntityKind",
		signature = "SynapseEntity",
		definition = function(entity){
			entity@synapseEntityKind
		}
)

#####
## Set the entity kind
#####
setMethod(
		f = "synapseEntityKind<-",
		signature = "SynapseEntity",
		definition = function(entity, value){
			entity@synapseEntityKind <- value
			return(entity)
		}
)

#####
## Get the entity from Synapse
#####
setMethod(
		f = "refreshEntity",
		signature = "SynapseEntity",
		definition = function(entity){
			getEntity(entity)
		}
)

#####
## Refresh the entities annotations
#####
setMethod(
		f = "refreshAnnotations",
		signature = "SynapseEntity",
		definition = function(entity){
			annotations(entity) <- do.call(class(annotations(entity)), list(entity = getAnnotations(.extractEntityFromSlots(entity))))
			return(entity)
		}		
)
	


