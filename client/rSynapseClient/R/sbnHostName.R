sbnHostName <- function(host){
	config <- getClientConfig()
	if (!missing(host)) {
		config@host <- host
		setClientConfig(config)
	}
	else {
		return(config@host)
	}
}

