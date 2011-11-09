.synapseGetDelete <- 
		function(uri, requestMethod, host = .getRepoEndpointLocation(), curlHandle=getCurlHandle(), 
				anonymous = .getCache("anonymous"), path = .getRepoEndpointPrefix(), opts = .getCache("curlOpts"), entity=NULL)
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
	
	## Add the provenance parameter, if applicable
	step <- .getCache("currentStep")
	if(!is.null(step)) {
		if(grepl("?", uri, fixed=TRUE)) {
			uri <- paste(uri, "&stepId=", propertyValue(step, "id"), sep="")
		} else {
			uri <- paste(uri, "?stepId=", propertyValue(step, "id"), sep="")			
		}
	}
	
	if(length(path) > 1)
		stop("put", paste(length(path), path))
	## Prepare the header. If not an anonymous request, stuff the
	## sessionToken into the header
	header <- .getCache("curlHeader")
	if(!anonymous) {
		header <- switch(authMode(),
						auth = .stuffHeaderAuth(header),
						hmac = .stuffHeaderHmac(header, uri),
						stop("Unknown auth mode: %s. Could not build header", authMod())
		)		
	}
	
	## Submit request and check response code
	d = debugGatherer()
	
	##curlSetOpt(opts,curl=curlHandle)
	if(is.null(entity)){
        if(.getCache("debug")) {
		    message("request: ", requestMethod, " ", uri)
	    }
		response <- getURL(uri, 
				customrequest = requestMethod, 
				httpheader = header, 
				curl = curlHandle, 
				debugfunction=d$update,
				.opts=opts
		)
	}else{
		httpBody <- toJSON(entity)
       	if(.getCache("debug")) {
            message("request: ", requestMethod, " ", uri)
		    message("requestBody: ", httpBody)
	    }
		response <- getURL(uri, 
				postfields = httpBody, 
				customrequest = requestMethod, 
				httpheader = header, 
				curl = curlHandle, 
				debugfunction=d$update,
				.opts=opts
		)
	}
	
	
	if(.getCache("debug")) {
		message(d$value())
		message("responseBody: ", response)
	}
	
	.checkCurlResponse(curlHandle, response)
	
	if("GET" == requestMethod) {
		## Parse response and prepare return value
		as.list(fromJSON(response))
	}
}

