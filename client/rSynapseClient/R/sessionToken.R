sessionToken <- 
		function(session.token, check.validity=FALSE, refresh.duration = .getCache("sessionRefreshDurationMin"))
{
	if (!missing(session.token)) {
		if(is.null(session.token)) session.token <- ""
		if(check.validity){
			refreshSessionToken(session.token)
		}
		.setCache("session.token", session.token)
	}else {
		session.token <- .getCache("session.token")
		elapsed.time.min <-  (as.numeric(Sys.time()) - as.numeric(.getCache("sessionTimestamp")))/60
		if(!is.null(.getCache("sessionTimestamp")) 
				&(check.validity || elapsed.time.min >= refresh.duration)){
			refreshSessionToken(session.token)
		}
		if(is.null(session.token)) session.toke <- ""
		return(session.token)
	}
}

