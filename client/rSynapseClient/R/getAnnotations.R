getAnnotations <- 
		function(id, curlHandle = getCurlHandle(), anonymous = .getCache("anonymous"))
{
	## constants
	kService <- "dataset"
	kUri <- "annotations"
	## end constants
	
	if(length(id) != 1){
		stop("multiple IDs provided")
	}
	
	uri <- paste(kService, id, kUri, sep="/")
	synapseGet(uri = uri, curlHandle = curlHandle, anonymous = anonymous)
}

