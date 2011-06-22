synapseAuthServiceEndpoint <- 
		function(endpoint)
{
	if (!missing(endpoint)) {
		.setCache("authservice.endpoint", endpoint)
		url <- URL(url=endpoint)
		.setCache("authservice.endpointLocation", paste(url@protocol, '://', url@authority, sep=''))
		.setCache("authservice.endpointPrefix", url@path)
	}
	else {
		return(.getCache("authservice.endpoint"))
	}
}

.getAuthEndpointLocation <- function() {
	.getCache("authservice.endpointLocation")	
}

.getAuthEndpointPrefix <- function() {
	.getCache("authservice.endpointPrefix")	
}