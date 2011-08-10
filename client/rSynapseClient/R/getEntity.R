.getEntity <- 
		function(kind, entity)
{	
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
	
	synapseGet(uri=uri, anonymous=FALSE)
}

setMethod(
		f = "getEntity",
		signature = "list",
		definition = function(entity){
			if(any(names(entity) == ""))
				stop("all entity elements must be named")
			if(!("uri" %in% names(entity)))
				stop("entity must contain an element named uri")
			splits <- strsplit(entity$uri, "/")[[1]]
			className <- splits[length(splits)-1]
			className <- sprintf("%s%s", toupper(substr(className, 1, 1)), tolower(gsub("^.", "", className)))
			do.call(className, args=list(entity=entity))
		}
)

setMethod(
		f = "getEntity",
		signature = signature("character"),
		definition = function(entity, kind){
			entity <- .getEntity(kind = kind, entity = entity)
			getEntity(entity)
		}
)

setMethod(
		f = "getEntity",
		signature = signature("numeric"),
		definition = function(entity, kind){
			entity <- .getEntity(kind = kind, entity = as.character(entity))
			getEntity(entity)
		}
)

setMethod(
		f = "getEntity",
		signature = "SynapseEntity",
		definition = function(entity){
			## refresh main entity
			entity <- do.call(class(entity), list(entity = .getEntity(kind = synapseEntityKind(entity), entity=.extractEntityFromSlots(entity))))
			
			## refresh annotations
			refreshAnnotations(entity)
		}
)

# TODO can we dynamically generate these functions?

getDataset <- 
		function(entity)
{
	.getEntity(kind="dataset", entity=entity)
}

getLayer <- 
		function(entity)
{
	.getEntity(kind="layer", entity=entity)
}

getLocation <- 
		function(entity)
{
	.getEntity(kind="location", entity=entity)
}

getPreview <- 
		function(entity)
{
	.getEntity(kind="preview", entity=entity)
}

getProject <- 
		function(entity)
{
	.getEntity(kind="project", entity=entity)
}
