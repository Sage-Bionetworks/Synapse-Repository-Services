.createEntity <- 
		function(kind, entity, curlHandle = getCurlHandle(), anonymous = .getCache("anonymous"))
{
	uri <- paste("/", kind, sep = "")
	
	synapsePost(uri=uri, entity=entity, curlHandle=curlHandle, anonymous=anonymous)
}

# TODO can we dynamically generate these functions?

createDataset <- 
		function(entity, curlHandle = getCurlHandle(), anonymous = .getCache("anonymous"))
{
	.createEntity("dataset", entity, curlHandle, anonymous)
}

createLayer <- 
		function(entity, curlHandle = getCurlHandle(), anonymous = .getCache("anonymous"))
{
	.createEntity("layer", entity, curlHandle, anonymous)
}

createLocation <- 
		function(entity, curlHandle = getCurlHandle(), anonymous = .getCache("anonymous"))
{
	.createEntity("location", entity, curlHandle, anonymous)
}

createPreview <- 
		function(entity, curlHandle = getCurlHandle(), anonymous = .getCache("anonymous"))
{
	.createEntity("preview", entity, curlHandle, anonymous)
}

createProject <- 
		function(entity, curlHandle = getCurlHandle(), anonymous = .getCache("anonymous"))
{
	.createEntity("project", entity, curlHandle, anonymous)
}
