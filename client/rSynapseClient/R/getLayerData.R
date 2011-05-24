getLayerData <-
		function(layer, locationType = "awsS3Location", curlHandle=getCurlHandle(), anonymous = TRUE)
{
	kHeader <- c(Accept = "application/json")
	kSupportedLocations <- c("awsS3Location")
	
	if(!(locationType %in% kSupportedLocations)){
		stop(paste("unsupported repository location:", locationType))
	}
	
	uri <- paste(sbnHostName(), layer$uri, locationType, sep='/')
	response <- getURL(uri, httpheader = kHeader, curl = curlHandle)
	checkCurlResponse(curlHandle, response)
	
	datapath <- fromJSON(response)$path
	zipFile <- gsub("\\?.+$", "", strsplit(datapath, ".com/")[[1]][2])
    
	# deflaux: avoid an SSL certificate warning by fixing the https
	# url, TODO the real fix is to do this further back in the service
	fixeddatapath <- sub("data01.sagebase.org.s3.amazonaws.com",
	              "s3.amazonaws.com/data01.sagebase.org", datapath)
    
	sbn.download.file(fixeddatapath, zipFile, method="curl")

	# deflaux: I could not figure out the right set of args to unzip
	# to get it to happily extract the directory structure contained
	# in the zip file
	
	
	extractDirectory <- paste(getwd(), strsplit(zipFile, ".zip")[[1]][1], sep="/")
	unzip(zipFile, exdir=extractDirectory, junkpaths=TRUE)
	files <- file.path(extractDirectory,list.files(extractDirectory))
	
	class(files) <- "layerData"
	attr(files, "layerType") <- layer$name
	return(files)
}
