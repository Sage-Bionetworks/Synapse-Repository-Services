synapseRepoServiceEndpoint <- 
		function(endpoint)
{
	if (!missing(endpoint)) {
		.setCache("reposervice.endpoint", endpoint)
		url <- .ParsedUrl(url=endpoint)
		.setCache("reposervice.endpointLocation", paste(url@protocol, '://', url@authority, sep=''))
		.setCache("reposervice.endpointPrefix", url@path)
	}
	else {
		return(.getCache("reposervice.endpoint"))
	}
}

.getRepoEndpointLocation <- function() {
	.getCache("reposervice.endpointLocation")	
}

.getRepoEndpointPrefix <- function() {
	.getCache("reposervice.endpointPrefix")	
}