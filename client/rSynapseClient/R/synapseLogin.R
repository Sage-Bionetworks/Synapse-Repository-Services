synapseLogin <- 
		function(username, password, host = synapseAuthServiceHostName(), path = .getCache("authServicePath"))
{
	#constants
	kService <- "session"
	#end constants
	
	#build the header containing username and password
	httpBody <- paste('{\"email\":\"', 
			username, 
			'\", \"password\":\"', 
			password, '\"}', 
			sep='')
	
	#Login and check for success
	response <- synapsePost(uri = kService, 
					httpBody = httpBody, 
					host = host, 
					path = path
				)
	
	#cache the session token. No need to check validity since it was just created
	sessionToken(response$sessionToken, check.validity = FALSE)
	.setCache("sessionTimestamp", Sys.time())
	cat(paste("Welcome ", response$displayName, "!\n", sep=""))
}
