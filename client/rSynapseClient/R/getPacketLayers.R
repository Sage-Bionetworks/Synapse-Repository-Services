getPacketLayers <-
		function(id, curlHandle=getCurlHandle(), anonymous=TRUE)
{
	kPath <- "repo/v1/dataset"
	kSuffix <- "layer"
	kHeader <- c(Accept = "application/json")

	uri <- paste(sbnHostName(), kPath, id, kSuffix, sep="/")
	response <- getURL(uri, httpheader = kHeader, curl = curlHandle)
	checkCurlResponse(curlHandle, response)
	
	layers <- fromJSON(response)[["results"]]

	for(i in 1:length(layers)){
		names(layers)[i] <- gsub(" ", ".", layers[[i]]$name)
		class(layers[[i]]) <- 'layer'
	}

	return(layers)
}
