synapseSessionToken <- 
		function(sessionToken, checkValidity=FALSE, refreshDuration = .getCache("sessionRefreshDurationMin"))
{
	if (!missing(sessionToken)) {
		if(is.null(sessionToken)) sessionToken <- ""
		if(checkValidity){
			synapseRefreshSessionToken(sessionToken)
		}
		.setCache("sessionToken", sessionToken)
	}else {
		sessionToken <- .getCache("sessionToken")
		elapsedTimeMin <-  (as.numeric(Sys.time()) - as.numeric(.getCache("sessionTimestamp")))/60
		if(!is.null(.getCache("sessionTimestamp")) 
				&(checkValidity || elapsedTimeMin >= refreshDuration)){
			synapseRefreshSessionToken(sessionToken)
		}
		if(is.null(sessionToken)) {
			sessionToken <- ""
		}
		return(sessionToken)
	}
}

