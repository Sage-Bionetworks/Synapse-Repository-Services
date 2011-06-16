.getEntity <- 
		function(kind, id, curlHandle = getCurlHandle(), anonymous = .getCache("anonymous"))
{
	if(length(id) != 1){
		stop("multiple ids provided")
	}
	
	uri <- paste(kind, id, sep = "/")
	
	## get reults and parse list
	results.list <- synapseGet(uri = uri, curlHandle = curlHandle, anonymous = anonymous)
	parseSingleRow(results.list)
}

getDataset <- 
		function(id, curlHandle = getCurlHandle(), anonymous = .getCache("anonymous"))
{
	.getEntity("dataset", id, curlHandle, anonymous)
}

getLayer <- 
		function(id, curlHandle = getCurlHandle(), anonymous = .getCache("anonymous"))
{
	.getEntity("layer", id, curlHandle, anonymous)
}

getLocation <- 
		function(id, curlHandle = getCurlHandle(), anonymous = .getCache("anonymous"))
{
	.getEntity("location", id, curlHandle, anonymous)
}

getPreview <- 
		function(id, curlHandle = getCurlHandle(), anonymous = .getCache("anonymous"))
{
	.getEntity("preview", id, curlHandle, anonymous)
}

getProject <- 
		function(id, curlHandle = getCurlHandle(), anonymous = .getCache("anonymous"))
{
	.getEntity("project", id, curlHandle, anonymous)
}
