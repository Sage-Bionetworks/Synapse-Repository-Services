getDataPacketSummary <- 
		function(id, curlHandle = getCurlHandle(), anonymous = .getCache("anonymous"))
{
	## constants
	kService <- "dataset"
	## end constants
	
	if(length(id) != 1){
		stop("multiple IDs provided")
	}
	
	uri <- paste(kservice, id, sep = "/")
	
	## get reults and parse list
	results.list <- synapseGet(uri = uri, curlHandle = curlHandle, anonymous = anonymous)
	parseSingleRow(results.list)
}

