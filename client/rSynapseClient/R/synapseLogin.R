synapseLogin <- 
		function(username = .getUsername(), password = .getPassword())
{
	## constants
	kService <- "/session"
	## end constants
	
	## get auth service endpoint and prefix from memory cache
	host = .getAuthEndpointLocation()
	path = .getAuthEndpointPrefix()

	entity <- list()
	entity$email <- username
	entity$password <- password
	
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

.getPassword <- function(){
	cat("Password: ")
	## Currently only suppresses output in unix-like terminals
	
	finallyCmd <- NULL
	if(tolower(.Platform$GUI) == "x11"){
		if(tolower(.Platform$OS.type) == "unix"){
			system("stty -echo")
			finallyCmd <- "stty echo"
		}
	}else if(tolower(.Platform$GUI == "rterm")){
		if(tolower(.Platform$OS.type) == "windows"){
			## TODO figure out how to suppress terminal output in Windows
		}
	}
	
	## TODO figure out how to "hide" password entry from GUIs
	tryCatch(
			password <- readline(),
			finally={
				if(!is.null(finallyCmd))
					system(finallyCmd) ## turn echo back on only if it was turned off
				cat("\n")
			}
	)
	return(password)
}

.getUsername <- function(){
	cat("Username: ")
	username <- readline()
	return(username)
}

