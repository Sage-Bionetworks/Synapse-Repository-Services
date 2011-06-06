synapsePostPut <- 
		function(uri, httpBody, requestMethod, host = synapseRepoServiceHostName(), curlHandle = getCurlHandle(), anonymous = .getCache("anonymous"), path = .getCache("repoServicePath"), opts = .getCache("curlOpts"))
{
	## constants
	kValidMethods <- c("POST", "PUT")
	## end constants
	
	if(!(requestMethod %in% kValidMethods)){
		stop("invalid request method")
	}
		
	if(!is.character(uri)){
		stop("a uri must be supplied of R type character")
	}
	
	## Prepare the header. If not an anonymous request, stuff the
	## session token into the header
	header <- .getCache("curlHeader")
	if(!anonymous){
		header <- c(header, sessionToken = sessionToken())
	}
	
	uri <- paste(host, path, uri, sep="/")
	
	## Submit request and check response code
	response <- getURL(uri, 
					postfields = httpBody, 
					customrequest = requestMethod, 
					httpheader = header, 
					curl = curlHandle, 
					.opts = opts
				)
				
	checkCurlResponse(curlHandle, response)
	
	## Parse response and prepare return value
	fromJSON(response)
}