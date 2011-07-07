getAnnotations <- 
		function(entity)
{
	if(missing(entity)) {
		stop("missing entity parameter")
	}

	if(!is.list(entity)){
		stop("the entity must be an R list")
	}
	
	if(!"annotations" %in% names(entity)){
		stop("the entity does not have an annotations uri")
	}
	
	synapseGet(uri=entity$annotations, anonymous=FALSE)
}

