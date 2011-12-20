synapseRepoServiceEndpoint <- 
		function(endpoint)
{
	if (!missing(endpoint)) {
		.setCache("reposerviceEndpoint", endpoint)
		url <- .ParsedUrl(url=endpoint)
		.setCache("reposerviceEndpointLocation", paste(url@protocol, '://', url@authority, sep=''))
		.setCache("reposerviceEndpointPrefix", url@path)
		.jenv[["syn"]]$setRepositoryEndpoint(endpoint)
	}
	else {
		return(.getCache("reposerviceEndpoint"))
	}
}

.getRepoEndpointLocation <- function() {
	.getCache("reposerviceEndpointLocation")	
}

.getRepoEndpointPrefix <- function() {
	.getCache("reposerviceEndpointPrefix")	
}