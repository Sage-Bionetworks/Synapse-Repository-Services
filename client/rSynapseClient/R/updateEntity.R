.updateEntity <- 
		function(kind, entity)
{
	if(missing(entity)) {
		stop("missing entity parameter")
	}
	
	if(!is.list(entity)){
		stop("the entity must be an R list")
	}
	
	if(!"uri" %in% names(entity)){
		stop("the entity does not have a uri")
	}
	
	synapsePut(uri=entity$uri, entity=entity, anonymous=FALSE)
}

# TODO can we dynamically generate these functions?

updateDataset <- 
		function(entity)
{
	.updateEntity(kind="dataset", entity=entity)
}

updateLayer <- 
		function(entity)
{
	.updateEntity(kind="layer", entity=entity)
}

updateLocation <- 
		function(entity)
{
	.updateEntity(kind="location", entity=entity)
}

updatePreview <- 
		function(entity)
{
	.updateEntity(kind="preview", entity=entity)
}

updateProject <- 
		function(entity)
{
	.updateEntity(kind="project", entity=entity)
}

setMethod(
		f = "updateEntity",
		signature = signature("SynapseEntity"),
		definition = function(entity){
			## update the entity and store the result
			oldAnnotations <- annotations(entity)
			entity <- do.call(class(entity), list(entity = .updateEntity(kind = synapseEntityKind(entity), entity=.extractEntityFromSlots(entity))))
			
			## merge annotations
			newAnnotations <- annotations(entity)
			annotationValues(newAnnotations) <- as.list(oldAnnotations)
			annotations(entity) <- newAnnotations
			
			tryCatch(entity <- updateAnnotations(entity),
					error = function(e){
						cat("Failed to update Annotations. Manually merge modifications into new object retrieved from database and try update again\n")
						stop(e)
					}
			)
			entity
		}
)
setMethod(
		f = "updateEntity",
		signature = signature("SynapseAnnotation"),
		definition = function(entity){
			entity <- updateAnnotations(.extractEntityFromSlots(entity))
			SynapseAnnotation(entity)
		}
)

######
## Creating Layer entities must be handled differently since get and post are asymmetrical.
## Specifically, after updating the annotations on the newly created enity, the etag is changed
## so we must refresh the entity before returning it. For locations, this refevts the url and md5sum
## to the values for the new Location, which were returned by the createEntity call.
######
setMethod(
		f = "updateEntity",
		signature = "Location",
		definition = function(entity){
			## create the entity
			if(length(as.list(annotations(entity))) > 0L)
				warning("Annotations can not be automatically persisted for Location entities and so are being discarded")
			entity <- do.call(class(entity), list(entity = .updateEntity(kind = synapseEntityKind(entity), entity=.extractEntityFromSlots(entity))))
		}
)

setMethod(
		f = "updateEntity",
		signature = "CachedLocation",
		definition = function(entity){
			oldClass <- class(entity)
			class(entity) <- "Location"
			updatedEntity <- updateEntity(entity)
			class(updatedEntity) <- oldClass
			class(entity) <- oldClass
			updatedEntity@cacheDir <- entity@cacheDir
			updatedEntity@files <- entity@files
			updatedEntity
		}
)

setMethod(
		f = "updateEntity",
		signature = "Layer",
		definition = function(entity){
			oldClass <- class(entity)
			class(entity) <- "SynapseEntity"
			updatedEntity <- updateEntity(entity)
			class(updatedEntity) <- oldClass
			class(entity) <- oldClass
			updatedEntity@location <- entity@location
			updatedEntity@objects <- entity@objects
			updatedEntity
		}
)