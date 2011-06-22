.getChildEntities <- 
		function(entity, childKind, curlHandle = getCurlHandle(), anonymous = .getCache("anonymous"))
{
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
