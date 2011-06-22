.synapsePostPut <- 
		function(uri, entity, requestMethod, host = synapseRepoServiceHostName(), curlHandle = getCurlHandle(), anonymous = .getCache("anonymous"), path = .getCache("repoServicePath"), opts = .getCache("curlOpts"))
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
	
	if(!is.list(entity)) {
		stop("an entity must be supplied of R type list")
	}
	
	httpBody <- .toJSON(entity)
	
	## Prepare the header. If not an anonymous request, stuff the
	## session token into the header
	header <- .getCache("curlHeader")
	if(!anonymous){
		header <- c(header, sessionToken = sessionToken())
	}
	if("PUT" == requestMethod) {
		# Add the ETag header
		header <- c(header, ETag = entity$etag)
	}
	
	# uris formed by the service already have their servlet prefix
	if(grepl(path, uri)) {
		uri <- paste(host, uri, sep="")
	}
	else {
		uri <- paste(host, path, uri, sep="")
	}
	
	## Submit request and check response code
	d = debugGatherer()
	response <- getURL(uri, 
			postfields = httpBody, 
			customrequest = requestMethod, 
			httpheader = header, 
			curl = curlHandle, 
			debugfunction=d$update, 
			.opts = opts
	)
	if(.getCache("debug")) {
		message(d$value())
		message("requestBody: ", httpBody)
		message("responseBody: ", response)
	}
	
	checkCurlResponse(curlHandle, response)
	
	## Parse response and prepare return value
	tryCatch(
			fromJSON(response),
			error = function(e){NULL}
	)
}

# rjson does not correctly serialize empty lists, it represents them as empty arrays instead of empty dictionaries
.toJSON <-
		function (x) 
{
	if (is.factor(x) == TRUE) {
		tmp_names <- names(x)
		x = as.character(x)
		names(x) <- tmp_names
	}
	if (!is.vector(x) && !is.null(x) && !is.list(x)) {
		x <- as.list(x)
		warning("JSON only supports vectors and lists - But I'll try anyways")
	}
	if (is.null(x)) 
		return("null")
	if (is.null(names(x)) == FALSE) {
		x <- as.list(x)
	}
	if (is.list(x) && !is.null(names(x))) {
		if (any(duplicated(names(x)))) 
			stop("A JSON list must have unique names")
		str = "{"
		first_elem = TRUE
		for (n in names(x)) {
			if (first_elem) 
				first_elem = FALSE
			else str = paste(str, ",", sep = "")
			str = paste(str, deparse(n), ":", .toJSON(x[[n]]), 
					sep = "")
		}
		str = paste(str, "}", sep = "")
		return(str)
	}
	if (length(x) == 0 && is.list(x)) {
		# Force empty lists to be represented as empty dictionaries in JSON
		return("{}")
	}
	if (length(x) != 1 || is.list(x)) {
		if (!is.null(names(x))) 
			return(.toJSON(as.list(x)))
		str = "["
		first_elem = TRUE
		for (val in x) {
			if (first_elem) 
				first_elem = FALSE
			else str = paste(str, ",", sep = "")
			str = paste(str, .toJSON(val), sep = "")
		}
		str = paste(str, "]", sep = "")
		return(str)
	}
	if (is.nan(x)) 
		return("\"NaN\"")
	if (is.na(x)) 
		return("\"NA\"")
	if (is.infinite(x)) 
		return(ifelse(x == Inf, "\"Inf\"", "\"-Inf\""))
	if (is.logical(x)) 
		return(ifelse(x, "true", "false"))
	if (is.character(x)) 
		return(gsub("\\/", "\\\\/", deparse(x)))
	if (is.numeric(x)) 
		return(as.character(x))
	stop("shouldnt make it here - unhandled type not caught")
}
