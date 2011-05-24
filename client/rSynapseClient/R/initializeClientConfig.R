initializeClientConfig <- function(host, sslhost){
	if(!missing(host) & !missing(sslhost)){
		client.config <- new(Class='ClientConfig', host=host, sslhost=sslhost)	
	}else if(!missing(host)){
		client.config <- new(Class='ClientConfig', host=host)
	}else if(!missing(sslhost)){
		client.config <- new(Class='ClientConfig', sslhost=sslhost)
	}else {
		client.config <- new(Class = "ClientConfig")
	}
	setClientConfig(client.config)
}
