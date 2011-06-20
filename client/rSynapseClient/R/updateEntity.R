.updateEntity <- 
		function(kind, entity, curlHandle = getCurlHandle(), anonymous = .getCache("anonymous"))
{
	
	synapsePut(uri=entity$uri, entity=entity, curlHandle=curlHandle, anonymous=anonymous)
}

updateDataset <- 
		function(entity, curlHandle = getCurlHandle(), anonymous = .getCache("anonymous"))
{
	.updateEntity("dataset", entity, curlHandle, anonymous)
}

updateLayer <- 
		function(entity, curlHandle = getCurlHandle(), anonymous = .getCache("anonymous"))
{
	.updateEntity("layer", entity, curlHandle, anonymous)
}

updateLocation <- 
		function(entity, curlHandle = getCurlHandle(), anonymous = .getCache("anonymous"))
{
	.updateEntity("location", entity, curlHandle, anonymous)
}

updatePreview <- 
		function(entity, curlHandle = getCurlHandle(), anonymous = .getCache("anonymous"))
{
	.updateEntity("preview", entity, curlHandle, anonymous)
}

updateProject <- 
		function(entity, curlHandle = getCurlHandle(), anonymous = .getCache("anonymous"))
{
	.updateEntity("project", entity, curlHandle, anonymous)
}
