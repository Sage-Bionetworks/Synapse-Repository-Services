synapseRefreshSessionToken <- 
		function(sessionToken, host = .getAuthServiceEndpointLocation())
{
	# constants
	kService <- "/session"
	## end constants
			
	entity <- list()
	entity$sessionToken <- sessionToken

	uri <- kService
	response <- synapsePut(uri = uri, entity = entity, path = .getAuthEndpointPrefix(), host = host, anonymous = TRUE)
	.setCache("sessionTimestamp", Sys.time())
}
