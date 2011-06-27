.getChildEntities <- 
		function(entity, childKind, curlHandle = getCurlHandle(), anonymous = .getCache("anonymous"))
{
	if(!is.list(entity)){
		stop("the entity must be an R list")
	}
	
	if(!"uri" %in% names(entity)){
		stop("the entity does not have a uri")
	}
	
	## TODO figure out how to in R do something like entity${$childKind}
	
	uri <- paste(entity$uri, childKind, sep = "/")
	synapseGet(uri = uri, curlHandle = curlHandle, anonymous = anonymous)
}

# TODO can we dynamically generate these functions?

getProjectDatasets <- 
		function(entity, curlHandle = getCurlHandle(), anonymous = .getCache("anonymous"))
{
	.getChildEntities(entity=entity, childKind="dataset", curlHandle = curlHandle, anonymous = anonymous)
}

getDatasetLayers <- 
		function(entity, curlHandle = getCurlHandle(), anonymous = .getCache("anonymous"))
{
	.getChildEntities(entity=entity, childKind="layer", curlHandle = curlHandle, anonymous = anonymous)
}

getLayerLocations <- 
		function(entity, curlHandle = getCurlHandle(), anonymous = .getCache("anonymous"))
{
	.getChildEntities(entity=entity, childKind="location", curlHandle = curlHandle, anonymous = anonymous)
}

getLayerPreviews <- 
		function(entity, curlHandle = getCurlHandle(), anonymous = .getCache("anonymous"))
{
	.getChildEntities(entity=entity, childKind="preview", curlHandle = curlHandle, anonymous = anonymous)
}
