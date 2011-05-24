sbnSslHostName <- function(sslhost){
	config <- getClientConfig()
	if (!missing(sslhost)) {
		config@sslhost <- sslhost
		setClientConfig(config)
	}
	else {
		return(config@sslhost)
	}
}
