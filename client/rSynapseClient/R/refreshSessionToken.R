refreshSessionToken <- 
		function(session.token, host = synapseAuthServiceHostName())
{
	# constants
	kService <- "session"
	## end constants
			
	entity <- list()
	entity$sessionToken <- session.token

	uri <- kService
	response <- synapsePut(uri = uri, entity = entity, path = .getCache("authServicePath"), host = host)
	.setCache("sessionTimestamp", Sys.time())
}
