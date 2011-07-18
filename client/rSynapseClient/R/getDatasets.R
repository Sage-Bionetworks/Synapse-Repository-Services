getDatasets <- 
		function(queryParams)
{
	if(missing(queryParams)){
		queryParams <- list()
	}
	if(!is.list(queryParams)){
		stop("params must be a list")
	}
	
	## constants
	kService <- "/query?query"
	kQueryRoot <- "select * from dataset"
	## end constants
	
	## add queary parameters to the uri
	paramString <- paste(names(queryParams), queryParams, sep=" ", collapse=" ")
	query <- sprintf("%s %s", kQueryRoot, paramString)
	uri <- sprintf("%s=%s", kService, curlEscape(query))
	
	jsonRecords <- synapseGet(uri=uri, anonymous=FALSE)
	result <- .parseJSONRecords(jsonRecords$results)
	attr(result, "totalNumberOfResults") <- jsonRecords$totalNumberOfResults
	
	## drop the dataset prefix from field names
	names(result) <- gsub("^dataset[\\.]", "", names(result))

	return(result)
}

