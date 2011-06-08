synapseGet <- 
		function(uri, host = synapseRepoServiceHostName(), curlHandle=getCurlHandle(), anonymous = .getCache("anonymous"), path = .getCache("repoServicePath"), opts = .getCache("curlOpts"))
{
	
	if(!is.character(uri)){
		stop("a uri must be supplied of R type character")
	}

	# uris formed by the service already have their servlet prefix
	if(grepl(path, uri)) {
		uri <- paste(host, uri, sep="/")
	}else {
		uri <- paste(host, path, uri, sep="/")
	}
	
	## Prepare the header. If not an anonymous request, stuff the
	## session token into the header
	header <- .getCache("curlHeader")
	if(!anonymous){
		header <- c(header, sessionToken = sessionToken())
	}
	
	## Submit request and check response code
	response <- getURL(uri, 
					httpheader = header, 
					curl = curlHandle, 
					.opts = opts
				)
				
	checkCurlResponse(curlHandle, response)
	
	## Parse response and prepare return value
	fromJSON(response)
}

