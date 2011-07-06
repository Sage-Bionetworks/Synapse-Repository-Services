.createEntity <- 
		function(kind, entity)
{
	if(!is.list(entity)){
		stop("the entity must be an R list")
	}
	
	uri <- paste("/", kind, sep = "")
	
	synapsePost(uri=uri, entity=entity, anonymous=FALSE)
}

# TODO can we dynamically generate these functions?

createDataset <- 
		function(entity)
{
	.createEntity(kind="dataset", entity=entity)
}

createLayer <- 
		function(entity)
{
	.createEntity(kind="layer", entity=entity)
}

createLocation <- 
		function(entity)
{
	.createEntity(kind="location", entity=entity)
}

createPreview <- 
		function(entity)
{
	.createEntity(kind="preview", entity=entity)
}

createProject <- 
		function(entity)
{
	.createEntity(kind="project", entity=entity)
}
