.deleteEntity <- 
		function(kind, entity)
{
	
	if(missing(entity)) {
		stop("missing entity parameter")
	}
	
	# entity parameter is an entity	
	if(is.list(entity)){
		if(!"uri" %in% names(entity)){
			stop("the entity does not have a uri")
		}
		uri <- entity$uri
	}
	# entity parameter is an entity id
	else {
		if(length(entity) != 1){
			stop("pass an entity or a single entity id to this method")
		}
		uri <- paste("/", kind, entity, sep = "/")
	}	

	## No results are returned by this
	synapseDelete(uri=uri, anonymous=FALSE)
}

# TODO can we dynamically generate these functions?

deleteDataset <- 
		function(entity)
{
	.deleteEntity(kind="dataset", entity=entity)
}

deleteLayer <- 
		function(entity)
{
	.deleteEntity(kind="layer", entity=entity)
}

deleteLocation <- 
		function(entity)
{
	.deleteEntity(kind="location", entity=entity)
}

deletePreview <- 
		function(entity)
{
	.deleteEntity(kind="preview", entity=entity)
}

deleteProject <- 
		function(entity)
{
	.deleteEntity(kind="project", entity=entity)
}

setMethod(
		f = "deleteEntity",
		signature = "SynapseEntity",
		definition = function(entity){
			synapseDelete(uri=propertyValue(entity, "uri"))
		}
)

setMethod(
		f = "deleteEntity",
		signature = "list",
		definition = function(entity, kind){
			.deleteEntity(kind = kind, entity = entity)
		}
)

setMethod(
		f = "deleteEntity",
		signature = "numeric",
		definition = function(entity){
			deleteEntity(as.character(entity))
		}
)
setMethod(
		f = "deleteEntity",
		signature = "character",
		definition = function(entity){
			entity <- getEntity(entity)
			deleteEntity(entity)
		}
)

