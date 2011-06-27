getLayers <-
		function(entity=entity, returnS4Objects = FALSE, curlHandle = getCurlHandle(), anonymous = .getCache("anonymous"))
{

	if(!is.list(entity)){
		stop("the entity must be an R list")
	}
	
	if(!"layers" %in% names(entity)){
		stop("the entity does not have layers")
	}
	
	result <- synapseGet(uri = entity$layers, curlHandle = curlHandle, anonymous = anonymous)
	
	layers <- result$results

	returnVal <- NULL
	for(i in 1:length(layers)){
		names(layers)[i] <- layers[[i]]$type
		class(layers[[i]]) <- 'layerList'
		if(returnS4Objects) {
			returnVal <- c(returnVal, Layer(layers[[i]]))
		}
	}

	if(returnS4Objects) {
		return(returnVal)
	}

	return(layers)
}

