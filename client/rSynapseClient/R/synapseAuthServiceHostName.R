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
