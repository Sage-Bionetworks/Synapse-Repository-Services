synapseLogin <- 
		function(username, password, curlHandle = getCurlHandle(), host = .getAuthEndpointLocation(), path = .getAuthEndpointPrefix())
{
	#constants
	kService <- "/session"
	#end constants
	
	entity <- list()
	entity$email <- username
	entity$password <- password
	
	#Login and check for success
	response <- synapsePost(uri = kService, 
					entity = entity, 
					host = host, 
					path = path,
					curl = curlHandle
				)
	
	.checkCurlResponse(curlHandle, response)
	#cache the sessionToken. No need to check validity since it was just created
	synapseSessionToken(response$sessionToken, check.validity = FALSE)
	.setCache("sessionTimestamp", Sys.time())
	cat(paste("Welcome ", response$displayName, "!\n", sep=""))
}
