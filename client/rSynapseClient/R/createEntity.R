.createEntity <- 
		function(kind, entity)
{
	if(missing(entity)) {
		stop("missing entity parameter")
	}
	
	if(!is.list(entity)){
		stop("the entity must be an R list")
	}
	
	uri <- paste("/", kind, sep = "")
	
	synapsePost(uri=uri, entity=entity, anonymous = FALSE)
}

createDataset <- 
		function(entity)
{
	.createEntity(kind="dataset", entity=entity)
}

createLayer <- 
		function(entity)
{
	.createEntity(kind="layer", entity=entity)
}

createLocation <- 
		function(entity)
{
	.createEntity(kind="location", entity=entity)
}

createPreview <- 
		function(entity)
{
	.createEntity(kind="preview", entity=entity)
}

createProject <- 
		function(entity)
{
	.createEntity(kind="project", entity=entity)
}

setMethod(
		f = createEntity,
		signature = signature("list"),
		definition = function(entity, className){
			if(any(names(entity) == ""))
				stop("all entity elements must be named")
			kind <- synapseEntityKind(new(Class=className))
			entity <- .createEntity(kind=kind, entity)
			do.call(className, args = list(entity=entity))
		}
)

setMethod(
		f = "createEntity",
		signature = signature("SynapseEntity"),
		definition = function(entity){
			## create the entity
			oldAnnotations <- annotations(entity)
			oldClass <- class(entity)
			uri <- paste("/", synapseEntityKind(entity), sep = "")
			entity <- SynapseEntity(synapsePost(uri=uri, entity=.extractEntityFromSlots(entity)))
			class(entity) <- oldClass
			
			## update entity annotations
			newAnnotations <- annotations(entity)
			if(length(as.list(oldAnnotations)) > 0L){
				annotationValues(newAnnotations) <- as.list(oldAnnotations)
				tryCatch(
						annotations(entity) <- updateEntity(newAnnotations),
						error = function(e){
							## unable to update annotations. delete parent entity.
							deleteEntity(entity)
							stop("Could not set annotations: ", e)
						}
				)
				entity <- refreshEntity(entity)
			}	
			entity
		}
)

######
## Creating Layer entities must be handled differently since get and post are asymmetrical.
## Specifically, after updating the annotations on the newly created enity, the etag is changed
## so we must refresh the entity before returning it. For locations, this refevts the url and md5sum
## to the values for the new Location, which were returned by the createEntity call.
######
setMethod(
		f = "createEntity",
		signature = "Location",
		definition = function(entity){
			## create the entity
			if(length(as.list(annotations(entity))) > 0L)
				warning("Annotations can not be automatically be persisted for Location entities and so are being discarded")
			createEntity(entity = .extractEntityFromSlots(entity), className = class(entity))
		}
)

setMethod(
		f = "createEntity",
		signature = "CachedLocation",
		definition = function(entity){
			oldClass <- class(entity)
			class(entity) <- "Location"
			createdEntity <- createEntity(entity)
			class(createdEntity) <- oldClass
			class(entity) <- oldClass
			createdEntity@cacheDir <- entity@cacheDir
			createdEntity@files <- entity@files
			createdEntity
		}
)


setMethod(
		f = "createEntity",
		signature = "Layer",
		definition = function(entity){
			oldClass <- class(entity)
			class(entity) <- "SynapseEntity"
			createdEntity <- createEntity(entity)
			class(createdEntity) <- oldClass
			class(entity) <- oldClass
			createdEntity@location <- entity@location
			createdEntity@objects <- entity@objects
			createdEntity@synapseWebUrl <- .buildSynapseUrl(propertyValue(createdEntity, "id"))
			createdEntity
		}
)
