getDatasetLayers <-
		function(id, curlHandle = getCurlHandle(), anonymous = .getCache("anonymous"))
{
	kService <- "dataset"
	kSuffix <- "layer"
	
	uri <- paste(kService, id, kSuffix, sep="/")
	result <- synapseGet(uri = uri, curlHandle = curlHandle, anonymous = anonymous)
	
	layers <- result$results

	returnVal <- NULL
	for(i in 1:length(layers)){
		class(layers[[i]]) <- 'layerList'
		returnVal <- c(returnVal, Layer(layers[[i]]))
	}

	return(returnVal)
}

