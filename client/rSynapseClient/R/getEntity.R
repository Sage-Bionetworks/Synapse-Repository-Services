setMethod(
		f = ".getEntityInfo",
		signature = "character",
		definition = function(entity){
			uri <- sprintf("/entity/%s/type", entity)
			info <- synapseGet(uri=uri)
			
			## clean up slashes
			info$type <- gsub("/", "", info$type)
			info
		}
)

setMethod(
		f = ".getEntityInfo",
		signature = "numeric",
		definition = function(entity){
			.getEntityInfo(as.character(entity))
		}
)

setMethod(
		f = ".getEntityInfo",
		signature = "SynapseEntity",
		definition = function(entity){
			if(is.null(id <- propertyValue(entity, "id")))
				stop("entity must contain an id property value")
			if(!is.null(propertyValue(entity,"uri"))){
				splits <- strsplit(propertyValue(entity,"uri"),"/")
				return(list(kind = splits[[1]][length(splits[[1]]) - 1]))
			}
			.getEntityInfo(id)
		}
)

setMethod(
		f = ".getEntityInfo",
		signature = "list",
		definition = function(entity){
			if("uri" %in% names(entity)){
				splits <- strsplit(entity$uri,"/")
				return(list(kind = splits[[1]][length(splits[[1]]) - 1]))
			}
				
			if(!("id" %in% names(entity)))
				stop("entity must have a field named 'id'")
			if(is.null(entity$id))
				stop("entity id must not be null")
			.getEntityInfo(entity$id)
		}
)


.getEntity <- 
		function(entity)
{	
	if(is.null(entity))
		stop("entity cannot be null")
	entityInfo <- .getEntityInfo(entity)
	kind <-  entityInfo$type

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
		uri <- sprintf("/%s/%s", kind, entity)
	}	
	
	synapseGet(uri=uri)
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
			tryCatch(
				do.call(className, args=list(entity=entity)),
				error = function(e){
					SynapseEntity(entity)
				}
			)
		}
)

setMethod(
		f = "getEntity",
		signature = signature("character"),
		definition = function(entity){
			entity <- .getEntity(entity = entity)
			getEntity(entity)
		}
)

setMethod(
		f = "getEntity",
		signature = signature("numeric"),
		definition = function(entity){
			entity <- .getEntity(entity = as.character(entity))
			getEntity(entity)
		}
)

setMethod(
		f = "getEntity",
		signature = "SynapseEntity",
		definition = function(entity){
			## refresh main entity
			entity <- do.call(class(entity), list(entity = .getEntity(entity=.extractEntityFromSlots(entity))))
			
			## refresh annotations
			refreshAnnotations(entity)
		}
)

# TODO can we dynamically generate these functions?

getDataset <- 
		function(entity)
{
	.getEntity(entity=entity)
}

getLayer <- 
		function(entity)
{
	.getEntity(entity=entity)
}

getLocation <- 
		function(entity)
{
	.getEntity(entity=entity)
}

getPreview <- 
		function(entity)
{
	.getEntity(entity=entity)
}

getProject <- 
		function(entity)
{
	.getEntity(entity=entity)
}
