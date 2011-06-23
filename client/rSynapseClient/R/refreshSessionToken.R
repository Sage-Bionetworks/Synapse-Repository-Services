refreshSessionToken <- 
		function(session.token, host = .getAuthServiceEndpointLocation())
{
	# constants
	kService <- "/session"
	## end constants
			
	entity <- list()
	entity$sessionToken <- session.token

	uri <- kService
	response <- synapsePut(uri = uri, entity = entity, path = .getAuthEndpointPrefix(), host = host, anonymous = TRUE)
	.setCache("sessionTimestamp", Sys.time())
}
