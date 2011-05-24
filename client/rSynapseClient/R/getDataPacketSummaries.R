getDataPacketSummaries <- function(query.params = list(limit=10, offset=1), curl.handle=getCurlHandle(), anonymous = TRUE){
	if(!is.list(query.params)){
		stop("params must be a list")
	}
	# constants
	kPath <- "repo/v1/query?query"
	kQueryRoot <- "select+*+from+dataset"
	kHeader <- c(Accept = "application/json")
	# end constants
	
	#add queary parameters to the uri
	query <- kQueryRoot
	for(n in names(query.params)){
		query <- paste(query, n, query.params[n], sep='+')
	}
	uri <- paste(paste(sbnHostName(), kPath,sep="/"), query, sep="=")
	
	#prepare the header. If not an anonymouse request, stuff the session token into the header
	header <- kHeader
	if(!anonymous){
		header <- c(header, sessionToken = sessionToken())
	}
	
	#submit request and check response code
	
	response <- getURL(uri, httpheader = header, curl = curl.handle)
	checkCurlResponse(curl.handle, response)
	
	#parse response and prepare return value
	results.list <- fromJSON(response)
	return.val <- parseJSONRecords(results.list$results)
	attr(return.val, "total_records_available") <- results.list$totalNumberOfResults

	return(return.val)
}

