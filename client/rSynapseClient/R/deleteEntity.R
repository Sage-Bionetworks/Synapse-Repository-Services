.deleteEntityById <- 
		function(kind, id)
{
	if(length(id) != 1){
		stop("multiple ids provided")
	}
	
	uri <- paste("/", kind, id, sep = "/")
	
	## No results are returned by this
	synapseDelete(uri=uri, anonymous=FALSE)
}

.deleteEntity <- 
		function(kind, entity)
{
	if(!is.list(entity)){
		stop("the entity must be an R list")
	}
	
	.deleteEntityById(kind=kind, id=entity$id)
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

deleteDatasetById <- 
		function(id)
{
	.deleteEntityById(kind="dataset", id=id)
}

deleteLayerById <- 
		function(id)
{
	.deleteEntityById(kind="layer", id=id)
}

deleteLocationById <- 
		function(id)
{
	.deleteEntityById(kind="location", id=id)
}

deletePreviewById <- 
		function(id)
{
	.deleteEntityById(kind="preview", id=id)
}

deleteProjectById <- 
		function(id)
{
	.deleteEntityById(kind="project", id=id)
}
