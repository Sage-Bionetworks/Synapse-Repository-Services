synapseQuery <- 
		function(queryStatement, curlHandle=getCurlHandle(), anonymous = .getCache("anonymous"))
{
	# Constants
	kPath <- "query?query="
	# end constants
	
	if(!is.character(queryStatement)){
		stop("a query statement must be supplied of R type character")
	}
	
	uri <- paste(kPath, curlEscape(queryStatement), sep="/")
	
	result <- synapseGet(uri = uri, 
					curlHandle = curlHandle, 
					anonymous = anonymous
			  )
	
	# Parse response and prepare return value
	return.val <- parseJSONRecords(results$results)
	attr(return.val, "totalNumberOfResults") <- results$totalNumberOfResults

	return(return.val)
}

