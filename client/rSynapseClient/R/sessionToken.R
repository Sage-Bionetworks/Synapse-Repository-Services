synapseSessionToken <- 
		function(sessionToken, check.validity=FALSE, refresh.duration = .getCache("sessionRefreshDurationMin"))
{
	if (!missing(sessionToken)) {
		if(is.null(sessionToken)) sessionToken <- ""
		if(check.validity){
			synapseRefreshSessionToken(sessionToken)
		}
		.setCache("sessionToken", sessionToken)
	}else {
		sessionToken <- .getCache("sessionToken")
		elapsed.time.min <-  (as.numeric(Sys.time()) - as.numeric(.getCache("sessionTimestamp")))/60
		if(!is.null(.getCache("sessionTimestamp")) 
				&(check.validity || elapsed.time.min >= refresh.duration)){
			synapseRefreshSessionToken(sessionToken)
		}
		if(is.null(sessionToken)) session.toke <- ""
		return(sessionToken)
	}
}

