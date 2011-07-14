synapseSessionToken <- 
		function(sessionToken, checkValidity=FALSE, refreshDuration = .getCache("sessionRefreshDurationMin"))
{
	if (!missing(sessionToken)) {
		if(is.null(sessionToken)) sessionToken <- ""
		if(checkValidity){
			synapseRefreshSessionToken(sessionToken)
		}
		.setCache("sessionToken", sessionToken)
	} else {
		# This could be null if the user has not logged in, but that's ok
		sessionToken <-	.getCache("sessionToken")
		# Refresh sessionToken as applicable
		if(checkValidity) {
			synapseRefreshSessionToken(sessionToken)
		} else if(!is.null(.getCache("sessionTimestamp"))) {
			elapsedTimeMin <-  (as.numeric(Sys.time()) - as.numeric(.getCache("sessionTimestamp")))/60
			if(elapsedTimeMin >= refreshDuration) {
				synapseRefreshSessionToken(sessionToken)
			} 
		}
		return(sessionToken)
	}
}

