getPacketLayers <-
		function(id, curlHandle = getCurlHandle(), anonymous = .getCache("anonymous"))
{
	kService <- "dataset"
	kSuffix <- "layer"
	
	uri <- paste(kService, id, kSuffix, sep="/")
	result <- synapseGet(uri = uri, curlHandle = curlHandle, anonymous = anonymous)
	
	layers <- result$results

	for(i in 1:length(layers)){
		names(layers)[i] <- layers[[i]]$type
		class(layers[[i]]) <- 'layer'
	}

	return(layers)
}
