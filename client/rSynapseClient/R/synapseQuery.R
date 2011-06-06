synapseQuery <- function(queryStatement, curl.handle=getCurlHandle(), anonymous = FALSE){

	if(!is.character(queryStatement)){
		stop("a query statement must be supplied of R type character")
	}
	
	# Constants
	kPath <- "/repo/v1/query?query="
	kHeader <- c(Accept = "application/json")
	# end constants
	
	uri <- paste(sbnHostName(), kPath, curlEscape(queryStatement), sep="")
	
	# Prepare the header. If not an anonymous request, stuff the
	# session token into the header
	header <- kHeader
	if(!anonymous){
		header <- c(header, sessionToken = sessionToken())
	}
	
	# Submit request and check response code
	d = debugGatherer()
	response <- getURL(uri, debugfunction=d$update, verbose = TRUE, httpheader = header, curl = curl.handle)
    d$value()
	checkCurlResponse(curl.handle, response)
	
	# Parse response and prepare return value
	results.list <- fromJSON(response)
	return.val <- parseJSONRecords(results.list$results)
	attr(return.val, "totalNumberOfResults") <- results.list$totalNumberOfResults

	return(return.val)
}

