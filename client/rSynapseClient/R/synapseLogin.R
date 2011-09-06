synapseLogin <- 
		function(username = "", password = "")
{
	## username and password must both be of length
	if(any(length(username) !=1 || length(password) != 1))
		stop("Please provide a single username and password")
	
	## replace nulls with empty strings
	if(is.null(username))
		username <- ""
	if(is.null(password))
		password <- ""
	
	credentials <- list(username = username, password = password)
	
	if(all(username != "" && password != "")){
		## username and password were both provided log the use in
		message(.doLogin(credentials))
	}else{
		## check to see if the "useTk" option is set
		useTk <- .getCache("useTk")
		if(is.null(useTk)){ ## useTk isn't set
			useTk <- .decideTk()
		}
		
		##initiate login.
		if(useTk){
			message(.doTkLogin(credentials$username))
		}else{
			message(.doTerminalLogin(credentials$username))
		}
	}
}

.decideTk <-
		function()
{
	## if this is a unix terminal, do a terminal login
	if(tolower(.Platform$OS.type) == "unix"){
		if(tolower(.Platform$GUI) %in% c("aqua", "x11")){
			## don't use tk for terminal or for CRAN R GUI
			## the CRAN R GUI locks up when tk is initialized
			## if it is not installed properly
			useTk <- FALSE
		}else{
			## another GUI is being used. check to see if Tk is
			## installed
			useTk <- .hasTk()
		}
		
	}else{
		## this is a non OSX/unix OS. Tk installation that comes with
		## R should work.
		useTk <- .hasTk()
	}
	useTk
}


.doTerminalLogin <-
		function(username)
{
	credentials <- .terminalGetCredentials(username)
	.doLogin(credentials)
}

.doTkLogin <-
		function(username)
{
	credentials <- .tkGetCredentials(username)
	.doLogin(credentials)
}

.doLogin <- 
		function(credentials)
{
	## constants
	kService <- "/session"
	## end constants
	
	
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
	sprintf("Welcome %s!", response$displayName)
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


