.getChildEntities <- 
		function(entity, offset, limit, kind, childKind, includeParentAnnot = FALSE)
{
	# entity parameter is an entity id
	if(!is.list(entity)) {
		if(length(entity) > 1){
			return(rbind(.getChildEntities(entity[1], offset, limit, kind, childKind, includeParentAnnot), .getChildEntities(entity[-1], offset, limit, kind, childKind, includeParentAnnot)))
		}
		entity <- .getEntity(kind, entity)
	}
	
	if(!is.data.frame(entity)){
		## convert NULLs to strings
		indx <- as.numeric(which(unlist(lapply(entity, FUN=is.null))))
		if(length(indx) > 0)
			entity[indx] <- "NULL"
		entity <- data.frame(entity, stringsAsFactors = FALSE)
	}
	
	if(nrow(entity) > 1)
		return(rbind(.getChildEntities(entity[1,], offset, limit, kind, childKind, includeParentAnnot), .getChildEntities(entity[-1,], offset, limit, kind, childKind, includeParentAnnot)))
	
	if(is.null(entity$id)){
			warning("the entity does not have an id")
			return(NULL)
	}
	
	uri <- sprintf("/%s/%s/%s?limit=%s&offset=%s", kind, entity$id, childKind, limit, offset)
	
	response <- synapseGet(uri=uri, anonymous=FALSE)
	if(response$totalNumberOfResults == 0)
		return(NULL)
	
	children <- .jsonListToDataFrame(response$results)
	if(includeParentAnnot){
		indx <- which(as.logical(lapply(entity,FUN=is.null)))
		entity[indx] <- as.character(entity[indx])
		parent <- as.data.frame(entity, stringsAsFactors=FALSE)
		names(parent) <- paste(kind, names(parent), sep = ".")
		names(children) <- paste(childKind, names(children), sep=".")
		children <- merge(parent, children, by.x=paste(kind,"id",sep="."), by.y=paste(childKind, "parentId", sep="."), all=TRUE)
	}

	return(children)
}

# TODO can we dynamically generate these functions?

getProjectDatasets <- 
		function(entity, includeParentAnnot=TRUE, offset=1, limit=100)
{
	missing(entity)
	.getChildEntities(entity=entity, offset=offset, limit=limit, kind="project", childKind="dataset", includeParentAnnot = includeParentAnnot)
}

getDatasetLayers <- 
		function(entity, includeParentAnnot=TRUE, offset=1, limit=100)
{
	## If entity wasn't provided, return layers for all datasets
	if(missing(entity))
		entity <- getDatasets()$id
	.getChildEntities(entity=entity, offset=offset, limit=limit, kind="dataset", childKind="layer", includeParentAnnot = includeParentAnnot)
}

getLayerLocations <- 
		function(entity, offset=1, limit=100)
{
	.getChildEntities(entity=entity, offset=offset, limit=limit, kind="layer", childKind="location", includeParentAnnot = FALSE)
}

getLayerPreviews <- 
		function(entity, offset=1, limit=100)
{
	.getChildEntities(entity=entity, offset=offset, limit=limit, kind="layer", childKind="preview", includeParentAnnot = FALSE)
}

#setMethod(
#		f = "getChildEntity",
#		signature = "synapseEntity",
#		definition = function(entity){
#			stop("method not yet implemented")
#		}
#)


