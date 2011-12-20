synapseAuthServiceEndpoint <- 
		function(endpoint)
{
	if (!missing(endpoint)) {
		.setCache("authserviceEndpoint", endpoint)
		url <- .ParsedUrl(url=endpoint)
		.setCache("authserviceEndpointLocation", paste(url@protocol, '://', url@authority, sep=''))
		.setCache("authserviceEndpointPrefix", url@path)
		.jenv[["syn"]]$setAuthEndpoint(endpoint)
	}
	else {
		return(.getCache("authserviceEndpoint"))
	}
}

.getAuthEndpointLocation <- function() {
	.getCache("authserviceEndpointLocation")	
}

.getAuthEndpointPrefix <- function() {
	.getCache("authserviceEndpointPrefix")	
}