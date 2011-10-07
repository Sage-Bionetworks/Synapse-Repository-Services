.synapsePostPut <- 
		function(uri, entity, requestMethod, host = .getRepoEndpointLocation(), curlHandle = getCurlHandle(), 
				anonymous = FALSE, path = .getRepoEndpointPrefix, opts = .getCache("curlOpts"))
{
	## constants
	kValidMethods <- c("POST", "PUT", "DELETE")
	## end constants
	
	if(!(requestMethod %in% kValidMethods)){
		stop("invalid request method")
	}
	
	if(!is.character(uri)){
		stop("a uri must be supplied of R type character")
	}
	
	if(!is.list(entity)) {
		stop("an entity must be supplied of R type list")
	}
	
	if(any(names(entity) == "") || is.null(names(entity))){
		stop("all entity elements must be named")
	}
	
	## change dates to characters
	indx <- grep("date", tolower(names(entity)))
	indx <- indx[as.character(entity[indx]) != "NULL"]
	indx <- indx[names(entity)[indx] != "dateAnnotations"]
	for(ii in indx)
		entity[ii] <- as.character(entity[ii])
	
	httpBody <- toJSON(entity)
	
	## uris formed by the service already have their servlet prefix
	if(grepl(path, uri)) {
		uri <- paste(host, uri, sep="")
	}
	else {
		uri <- paste(host, path, uri, sep="")
	}
	
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
	if("PUT" == requestMethod) {
		# Add the ETag header
		header <- c(header, ETag = entity$etag)
	}
	
	if(length(path) > 1)
		stop("put", paste(length(path), path))
	## Submit request and check response code
	d = debugGatherer()
	
	##curlSetOpt(opts,curl=curlHandle)
	
	response <- getURL(uri, 
			postfields = httpBody, 
			customrequest = requestMethod, 
			httpheader = header, 
			curl = curlHandle, 
			debugfunction=d$update,
			.opts=opts
	)
	if(.getCache("debug")) {
		message(d$value())
		message("requestBody: ", httpBody)
		message("responseBody: ", response)
	}
	
	.checkCurlResponse(curlHandle, response)
	
	## Parse response and prepare return value
	tryCatch(
			as.list(fromJSON(response)),
			error = function(e){NULL}
	)
}
