# Unit tests for getParentEntity method
# 
# Author: matt furia
###############################################################################
.setUp <-
		function()
{
	## make a copy of the old cache
	oldCache <- synapseClient:::.cache
	newCache <- new.env(parent=parent.env(oldCache))
	for(key in ls(oldCache))
		assign(key,get(key,envir=oldCache), envir=newCache)
	
	## override getEntity method
	setMethod(
			f = "getEntity",
			signature = "character",
			definition = function(entity){
				if(entity == "1")
					stop("In .checkCurlResponse(curlHandle, response) :HTTP Error: 403 for request http://localhost:8080/services-repository-0.7-SNAPSHOT/repo/v1/entity/1/type{'reason':'devUser1@sagebase.org lacks read access to the requested object.'}")
				if(entity == "2")
					return(new(Class="Project", properties = list(name = "parent", id="2", parentId="1")))
				if(entity == "3")
					return(new(Class="Layer", properties = list(name = "parent", id="3", parentId="2")))
				
				stop("entity Not found")
			}
	)
	.setCache("oldCache", oldCache)
}

.tearDown <-
		function()
{
	oldCache <- .getCache("oldCache")
	# put back the overridden functions and original cache
	unloadNamespace("synapseClient")
	unloadNamespace("RCurl")
	assignInNamespace(".cache", oldCache, "synapseClient")
	attachNamespace("synapseClient")
}

unitTestGetParentEntity <-
		function()
{
	childEntity <- getEntity("3")
	parentEntity <- getParentEntity(childEntity)
	checkEquals(propertyValue(childEntity, "parentId"), propertyValue(parentEntity, "id"))
	grandParentEntity <- getParentEntity(parentEntity)
	checkTrue(is.null(grandParentEntity))
	
}

