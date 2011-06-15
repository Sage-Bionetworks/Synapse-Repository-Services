getDatasets <- 
		function(queryParams = list(limit=10, offset=1), curlHandle=getCurlHandle(), anonymous = .getCache("anonymous"))
{
	if(!is.list(queryParams)){
		stop("params must be a list")
	}
	
	## constants
	kService <- "query?query"
	kQueryRoot <- "select * from dataset"
	## end constants
	
	## add queary parameters to the uri
	query <- kQueryRoot
	for(n in names(queryParams)){
		query <- paste(query, n, queryParams[n], sep=' ')
	}
	uri <- paste(kService, curlEscape(query), sep="=")
	
	json.records <- synapseGet(uri = uri, curlHandle = curlHandle, anonymous = anonymous)
	result <- parseJSONRecords(json.records$results)
	attr(result, "total_records_available") <- json.records$totalNumberOfResults

	return(result)
}

