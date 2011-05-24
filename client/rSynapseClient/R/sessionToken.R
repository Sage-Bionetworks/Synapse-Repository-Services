sessionToken <- function(session.token, check.validity = TRUE){
	# the client config object holds the session token
	config <- getClientConfig()
	
	if (!missing(session.token)) {
		config@session.token <- session.token
		
		#check that a valid token was provided
		if(check.validity && !isTokenValid(config)){
			stop("The token provided is not valid! Changes were not saved.")
		}
		setClientConfig(config)
	}else {
		if(!isTokenValid(config)){
			#throw an exception and tell the user to log in
			stop("You must be logged in to do that!")
		}
		return(config@session.token)
	}
}
