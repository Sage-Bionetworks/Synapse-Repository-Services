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

