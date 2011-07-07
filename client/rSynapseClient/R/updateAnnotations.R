updateAnnotations <- 
		function(annotations)
{
	
	if(missing(annotations)) {
		stop("missing annotations parameter")
	}
	
	if(!is.list(annotations)){
		stop("the annotations entity must be an R list")
	}
	
	if(!"uri" %in% names(annotations)){
		stop("the annotations entity does not have a uri")
	}
	
	if(!grepl("annotations", annotations$uri)){
		stop("this is not an annotations entity")
	}

	## rjson deserializes arrays of length one to scalars instead of lists, before we send any annotations 
	## back to the service, make sure all scalars are converted to lists since the service expects all 
	## annotation values to be arrays instead of scalars
	for(key in names(annotations)){
		# This is one of our annotation buckets
		if(is.list(annotations[[key]])) {
			for(annotKey in names(annotations[[key]])) {
				if(!is.list(annotations[[key]][[annotKey]])) {
					annotations[[key]][[annotKey]] <- list(annotations[[key]][[annotKey]])
				}
			}
		}
	}
	
	synapsePut(uri=annotations$uri, entity=annotations, anonymous=FALSE)
}
