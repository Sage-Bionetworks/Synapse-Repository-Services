updateAnnotations <- 
		function(annotations, curlHandle = getCurlHandle(), anonymous = .getCache("anonymous"))
{
	
	if(!grepl("annotations", annotations$uri)){
		stop("this is not an annotations entity")
	}

	# rjson deserializes arrays of length one to scalars instead of lists, before we send any annotations 
	# back to the service, make sure all scalars are converted to lists
	for(key in names(annotations)){
		# This is one of our annotation buckets
		if('list' == class(annotations[[key]])) {
			for(annotKey in names(annotations[[key]])) {
				if('list' != class(annotations[[key]][[annotKey]])) {
					annotations[[key]][[annotKey]] <- list(annotations[[key]][[annotKey]])
				}
			}
		}
	}
	
	synapsePut(uri = annotations$uri, entity=annotations, curlHandle = curlHandle, anonymous = anonymous)
}
