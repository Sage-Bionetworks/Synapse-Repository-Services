synapseRepoServiceHostName <- 
		function(host)
{
	if (!missing(host)) {
		.setCache("reposervice.host", host)
	}
	else {
		return(.getCache("reposervice.host"))
	}
}

synapseRepoServiceEndpoint <- 
		function(endpoint)
{
	if (!missing(endpoint)) {
		.setCache("reposervice.endpoint", endpoint)
		url <- URL(url=endpoint)
		.setCache("reposervice.host", paste(url@protocol, '://', url@host))
		.setCache("repoServicePath", url@path)
	}
	else {
		return(.getCache("reposervice.endpoint"))
	}
}