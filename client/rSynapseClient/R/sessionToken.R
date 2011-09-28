sessionToken <- 
		function(sessionToken, checkValidity=FALSE, refreshDuration = .getCache("sessionRefreshDurationMin"))
{
	kAuthMode <- "auth"
	if (!missing(sessionToken)) {
		if(checkValidity){
			refreshSessionToken(sessionToken)
		}
		.setCache("sessionToken", sessionToken)
		authMode(kAuthMode)
	} else {
		# This could be null if the user has not logged in, but that's ok
		sessionToken <-	.getCache("sessionToken")
		if(is.null(sessionToken)) {
			stop("please log into Synapse")
		}
		# Refresh sessionToken as applicable
		if(checkValidity) {
			refreshSessionToken(sessionToken)
		} else if(!is.null(.getCache("sessionTimestamp"))) {
			elapsedTimeMin <-  (as.numeric(Sys.time()) - as.numeric(.getCache("sessionTimestamp")))/60
			if(elapsedTimeMin >= refreshDuration) {
				refreshSessionToken(sessionToken)
			} 
		}
		return(sessionToken)
	}
}

refreshSessionToken <- 
		function(sessionToken)
{
	# constants
	kService <- "/session"
	## end constants
	
	host = .getAuthEndpointLocation()
	
	entity <- list()
	entity$sessionToken <- sessionToken
	
	uri <- kService
	response <- synapsePut(uri=uri, entity=entity, path=.getAuthEndpointPrefix(), host=host, anonymous=TRUE)
	.setCache("sessionTimestamp", Sys.time())
}
