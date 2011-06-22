synapseAuthServiceHostName <- 
		function(host)
{
	if (!missing(host)) {
		.setCache("authservice.host", host)
	}
	else {
		return(.getCache("authservice.host"))
	}
}

synapseAuthServiceEndpoint <- 
		function(endpoint)
{
	if (!missing(endpoint)) {
		.setCache("authservice.endpoint", endpoint)
		url <- URL(url=endpoint)
		.setCache("authservice.host", paste(url@protocol, '://', url@host))
		.setCache("authServicePath", url@path)
	}
	else {
		return(.getCache("authservice.endpoint"))
	}
}