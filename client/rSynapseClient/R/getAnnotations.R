getAnnotations <- 
		function(entity, curlHandle = getCurlHandle(), anonymous = .getCache("anonymous"))
{
	
	if(!is.list(entity)){
		stop("the entity must be an R list")
	}
	
	if(!"annotations" %in% names(entity)){
		stop("the entity does not have annotations")
	}
	
	synapseGet(uri = entity$annotations, curlHandle = curlHandle, anonymous = anonymous)
}

