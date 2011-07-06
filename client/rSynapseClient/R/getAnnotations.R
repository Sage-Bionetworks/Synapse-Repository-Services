getAnnotations <- 
		function(entity)
{
	
	if(!is.list(entity)){
		stop("the entity must be an R list")
	}
	
	if(!"annotations" %in% names(entity)){
		stop("the entity does not have annotations")
	}
	
	synapseGet(uri=entity$annotations, anonymous=FALSE)
}

