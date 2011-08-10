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
		definition = function(entity, updateAnnotations=TRUE){
			## update the entity and store the result
			tryCatch(entity <- updateAnnotations(entity),
					error = function(e){
						cat("Failed to update Annotations. Manually merge modifications into new object retrieved from database and try update again\n")
						stop(e)
					}
			)
			
			do.call(class(entity), list(entity = .updateEntity(kind = synapseEntityKind(entity), entity=.extractEntityFromSlots(entity))))
		}
)

