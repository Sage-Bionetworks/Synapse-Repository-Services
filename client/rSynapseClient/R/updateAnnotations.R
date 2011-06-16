updateAnnotations <- 
		function(entity, curlHandle = getCurlHandle(), anonymous = .getCache("anonymous"))
{
	
	if(!grepl("annotations", entity$uri)){
		stop("this is not an annotations entity")
	}

	# rjson deserializes arrays of length one to scalars instead of lists, before we send any annotations 
	# back to the service, make sure all scalars are converted to lists
	for(key in names(entity)){
		# This is one of our annotation buckets
		if('list' == class(entity[[key]])) {
			for(annotKey in names(entity[[key]])) {
				if('list' != class(entity[[key]][[annotKey]])) {
					entity[[key]][[annotKey]] <- list(entity[[key]][[annotKey]])
				}
			}
		}
	}
	
	synapsePut(uri = entity$uri, entity=entity, curlHandle = curlHandle, anonymous = anonymous)
}
