.getChildEntities <- 
		function(entity, childKind)
{
	if(!is.list(entity)){
		stop("the entity must be an R list")
	}
	
	if(!"uri" %in% names(entity)){
		stop("the entity does not have a uri")
	}
	
	## TODO figure out how to in R do something like entity${$childKind}
	
	uri <- paste(entity$uri, childKind, sep = "/")
	synapseGet(uri=uri, anonymous=FALSE)
}

# TODO can we dynamically generate these functions?

getProjectDatasets <- 
		function(entity)
{
	.getChildEntities(entity=entity, childKind="dataset")
}

getDatasetLayers <- 
		function(entity)
{
	.getChildEntities(entity=entity, childKind="layer")
}

getLayerLocations <- 
		function(entity)
{
	.getChildEntities(entity=entity, childKind="location")
}

getLayerPreviews <- 
		function(entity)
{
	.getChildEntities(entity=entity, childKind="preview")
}
