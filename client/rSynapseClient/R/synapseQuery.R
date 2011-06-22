synapseQuery <- 
		function(queryStatement, curlHandle=getCurlHandle(), anonymous = .getCache("anonymous"))
{
	# Constants
	kPath <- "/query?query="
	# end constants
	
	if(!is.character(queryStatement)){
		stop("a query statement must be supplied of R type character")
	}
	
	uri <- paste(kPath, curlEscape(queryStatement), sep="")
	
	result <- synapseGet(uri = uri, 
					curlHandle = curlHandle, 
					anonymous = anonymous
			  )
	
	if(result$totalNumberOfResults == 0){
		return(NULL)
	}
	# Parse response and prepare return value
	return.val <- jsonListToDataFrame(result$results)
	attr(return.val, "totalNumberOfResults") <- result$totalNumberOfResults

	return(return.val)
}

