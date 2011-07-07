.getEntity <- 
		function(kind, entity)
{
	if(missing(entity)) {
		stop("missing entity parameter")
	}
	
	# entity parameter is an entity	
	if(is.list(entity)){
		if(!"uri" %in% names(entity)){
			stop("the entity does not have a uri")
		}
		uri <- entity$uri
	}
	# entity parameter is an entity id
	else {
		if(length(entity) != 1){
			stop("pass an entity or a single entity id to this method")
		}
		uri <- paste("/", kind, entity, sep = "/")
	}	
	
	synapseGet(uri=uri, anonymous=FALSE)
}

# TODO can we dynamically generate these functions?

getDataset <- 
		function(entity)
{
	.getEntity(kind="dataset", entity=entity)
}

getLayer <- 
		function(entity)
{
	.getEntity(kind="layer", entity=entity)
}

getLocation <- 
		function(entity)
{
	.getEntity(kind="location", entity=entity)
}

getPreview <- 
		function(entity)
{
	.getEntity(kind="preview", entity=entity)
}

getProject <- 
		function(entity)
{
	.getEntity(kind="project", entity=entity)
}
