.getChildEntities <- 
		function(entity, offset, limit, kind, childKind)
{
	if(missing(entity)) {
		stop("missing entity parameter")
	}

	# entity parameter is an entity	
	if(is.list(entity)){
		if(!"uri" %in% names(entity)){
			stop("the entity does not have a uri")
		}
		## TODO figure out how to in R do something like entity${$childKind}
		uri <- sprintf("%s/%s?limit=%s&offset=%s", entity$uri, childKind, limit, offset)
	}
	# entity parameter is an entity id
	else {
		if(length(entity) != 1){
			stop("pass an entity or a single entity id to this method")
		}
		uri <- sprintf("/%s/%s/%s?limit=%s&offset=%s", kind, entity, childKind, limit, offset)
	}	
	
	synapseGet(uri=uri, anonymous=FALSE)
}

# TODO can we dynamically generate these functions?

getProjectDatasets <- 
		function(entity, offset=1, limit=100)
{
	.getChildEntities(entity=entity, offset=offset, limit=limit, kind="project", childKind="dataset")
}

getDatasetLayers <- 
		function(entity, offset=1, limit=100)
{
	.getChildEntities(entity=entity, offset=offset, limit=limit, kind="dataset", childKind="layer")
}

getLayerLocations <- 
		function(entity, offset=1, limit=100)
{
	.getChildEntities(entity=entity, offset=offset, limit=limit, kind="layer", childKind="location")
}

getLayerPreviews <- 
		function(entity, offset=1, limit=100)
{
	.getChildEntities(entity=entity, offset=offset, limit=limit, kind="layer", childKind="preview")
}
