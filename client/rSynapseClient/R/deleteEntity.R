.deleteEntity <- 
		function(kind, id, curlHandle = getCurlHandle(), anonymous = .getCache("anonymous"))
{
	if(length(id) != 1){
		stop("multiple ids provided")
	}
	
	uri <- paste(kind, id, sep = "/")
	
	## No results are returned by this
	synapseDelete(uri = uri, curlHandle = curlHandle, anonymous = anonymous)
}

deleteDataset <- 
		function(id, curlHandle = getCurlHandle(), anonymous = .getCache("anonymous"))
{
	.deleteEntity("dataset", id, curlHandle, anonymous)
}

deleteLayer <- 
		function(id, curlHandle = getCurlHandle(), anonymous = .getCache("anonymous"))
{
	.deleteEntity("layer", id, curlHandle, anonymous)
}

deleteLocation <- 
		function(id, curlHandle = getCurlHandle(), anonymous = .getCache("anonymous"))
{
	.deleteEntity("location", id, curlHandle, anonymous)
}

deletePreview <- 
		function(id, curlHandle = getCurlHandle(), anonymous = .getCache("anonymous"))
{
	.deleteEntity("preview", id, curlHandle, anonymous)
}

deleteProject <- 
		function(id, curlHandle = getCurlHandle(), anonymous = .getCache("anonymous"))
{
	.deleteEntity("project", id, curlHandle, anonymous)
}
