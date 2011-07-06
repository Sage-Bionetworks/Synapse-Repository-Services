synapseLogin <- 
		function(username, password, host = .getAuthEndpointLocation(), path = .getAuthEndpointPrefix())
{
	## constants
	kService <- "/session"
	## end constants
	
	entity <- list()
	entity$email <- username
	entity$password <- password
	
	## Login and check for success
	response <- synapsePost(uri = kService, 
					entity = entity, 
					host = host, 
					path = path,
				)
	
	## Cache the sessionToken. No need to check validity since it was just created
	synapseSessionToken(response$sessionToken, checkValidity=FALSE)
	.setCache("sessionTimestamp", Sys.time())
	message(paste("Welcome ", response$displayName, "!\n", sep=""))
}
