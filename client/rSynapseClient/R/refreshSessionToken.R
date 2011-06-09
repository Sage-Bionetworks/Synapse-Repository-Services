refreshSessionToken <- 
		function(session.token, host = synapseAuthServiceHostName(), sessionToken = getSessionToken())
{
	# constants
	kService <- "session"
	## end constants
			
	entity <- list()
	entity$sessionToken <- session.token

	uri <- kService
	response <- synapsePut(uri = uri, entity = entity, path = .getCache("authServicePath"), host = host, sessionToken = sessionToken)
	checkCurlResponse(sessionToken, response)
	.setCache("sessionTimestamp", Sys.time())
}
