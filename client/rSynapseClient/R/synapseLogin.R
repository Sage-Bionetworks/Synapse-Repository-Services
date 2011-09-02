synapseLogin <- 
		function(username = "", password = "")
{
	## constants
	kService <- "/session"
	## end constants
	
	credentials <- list(username = username, password = password)
	
	if(password == ""){
		credentials <- getCredentials(username)
	} else if(username==""){
		credentials$username <- .getUsername()
	}
	
	## get auth service endpoint and prefix from memory cache
	host <- .getAuthEndpointLocation()
	path <- .getAuthEndpointPrefix()

	entity <- list()
	entity$email <- credentials$username
	entity$password <- credentials$password
	
	## Login and check for success
	response <- synapsePost(uri = kService, 
			entity = entity, 
			host = host, 
			path = path,
			anonymous = TRUE
	)
	
	## Cache the sessionToken. No need to check validity since it was just created
	sessionToken(response$sessionToken, checkValidity=FALSE)
	.setCache("sessionTimestamp", Sys.time())
	message(paste("Welcome ", response$displayName, "!", sep=""))
}

synapseLogout <-
		function(localOnly=FALSE)
{
	## constants
	kService <- "/session"
	## end constants
	
	## get auth service endpoint and prefix from memory cache
	host = .getAuthEndpointLocation()
	path = .getAuthEndpointPrefix()
	
	entity <- list(sessionToken = sessionToken())
	
	if(!localOnly){
		response <- synapseDelete(uri = kService,
				entity = entity,
				host = host,
				path = path
		)
	}
	sessionToken(NULL)
	message("Goodbye.")
}


