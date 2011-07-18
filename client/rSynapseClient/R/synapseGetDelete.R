.synapseGetDelete <- 
		function(uri, requestMethod, host = .getRepoEndpointLocation(), curlHandle=getCurlHandle(), 
				anonymous = .getCache("anonymous"), path = .getRepoEndpointPrefix(), opts = .getCache("curlOpts"))
{

	## constants
	kValidMethods <- c("GET", "DELETE")
	## end constants
	
	if(!(requestMethod %in% kValidMethods)){
		stop("invalid request method")
	}
	
	if(!is.character(uri)){
		stop("a uri must be supplied of R type character")
	}

	## uris formed by the service already have their servlet prefix
	if(grepl(path, uri)) {
		uri <- paste(host, uri, sep="")
	}else {
		uri <- paste(host, path, uri, sep="")
	}
	
	## Prepare the header. If not an anonymous request, stuff the
	## sessionToken into the header
	header <- .getCache("curlHeader")
	if(!anonymous && !is.null(sessionToken())) {
		header <- c(header, sessionToken = sessionToken())
	}
	
	## Submit request and check response code
	d = debugGatherer()
	response <- getURL(uri, 
			customrequest = requestMethod, 
			httpheader = header,
			curl = curlHandle, 
			debugfunction=d$update, 
			.opts = opts
	)
	if(.getCache("debug")) {
		message(d$value())
		message("responseBody: ", response)
	}
	
	.checkCurlResponse(curlHandle, response)
	
	if("GET" == requestMethod) {
		## Parse response and prepare return value
		rjson::fromJSON(response)
	}
}

