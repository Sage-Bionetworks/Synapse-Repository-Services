getLayerData <-
		function(layer, locationType = "awsS3Location", curlHandle = getCurlHandle(), anonymous = .getCache("anonymous"))
{
	## constants
	kHeader <- c(Accept = "application/json")
	kDownloadMethod <- "curl"
	## end constants
	
	## make sure that the location type for this layer is currently supported
	if(!(locationType %in% .getCache("supportedRepositoryLocationTypes"))){
		stop(paste("unsupported repository location:", locationType))
	}
	
	uri <- paste(layer$uri, locationType, sep='/')
	
	## get result. path is an empty string since the path is already included in 
	## the uri element of layer
	result <- synapseGet(uri = uri, curlHandle = curlHandle, anonymous = anonymous, path = "")
	
	datapath <- result$path
	zipFile <- gsub("\\?.+$", "", strsplit(datapath, ".com/")[[1]][2])
    
	## deflaux: avoid an SSL certificate warning by fixing the https
	## url, TODO the real fix is to do this further back in the service
	fixeddatapath <- sub("data01.sagebase.org.s3.amazonaws.com",
	              "s3.amazonaws.com/data01.sagebase.org", datapath)
    
	synapseDownloadFile(url = fixeddatapath, destfile = zipFile, method = kDownloadMethod)

	## deflaux: I could not figure out the right set of args to unzip
	## to get it to happily extract the directory structure contained
	## in the zip file
	
	
	extractDirectory <- paste(getwd(), strsplit(zipFile, ".zip")[[1]][1], sep="/")
	unzip(zipFile, exdir=extractDirectory, junkpaths=TRUE)
	files <- file.path(extractDirectory,list.files(extractDirectory))
	
	class(files) <- "layerData"
	attr(files, "layerType") <- layer$type
	return(files)
}
