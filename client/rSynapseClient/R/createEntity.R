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
	
	synapsePost(uri=uri, entity=entity, anonymous=FALSE)
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
		definition = function(entity, createAnnotations = FALSE){
			## create the entity
			oldAnnotations <- annotations(entity)
			entity <- createEntity(entity = .extractEntityFromSlots(entity), className = class(entity))
			## update entity annotations
			newAnnotations <- annotations(entity)
			annotationValues(newAnnotations) <- as.list(oldAnnotations)
			tryCatch(
				annotations(entity) <- updateEntity(newAnnotations),
				error = function(e){
					## unable to update annotations. delete parent entity.
					deleteEntity(entity)
					stop("Could not set annotations: ", e)
				}
			)
			entity
		}
)



