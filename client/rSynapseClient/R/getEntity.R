.getEntityById <- 
		function(kind, id)
{
	if(length(id) != 1){
		stop("multiple ids provided")
	}
	
	uri <- paste("/", kind, id, sep = "/")
	
	synapseGet(uri=uri, anonymous=FALSE)
}

# TODO can we dynamically generate these functions?

getDatasetById <- 
		function(id)
{
	.getEntityById(kind="dataset", id=id)
}

getLayerById <- 
		function(id)
{
	.getEntityById(kind="layer", id=id)
}

getLocationById <- 
		function(id)
{
	.getEntityById(kind="location", id=id)
}

getPreviewById <- 
		function(id)
{
	.getEntityById(kind="preview", id=id)
}

getProjectById <- 
		function(id)
{
	.getEntityById(kind="project", id=id)
}
