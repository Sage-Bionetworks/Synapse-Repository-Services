synapseDownloadFile  <- 
		function (url, checksum, curlHandle = getCurlHandle(), cacheDir = synapseCacheDir(), opts = .getCache("curlOpts"))
{
	## Download the file to the cache
	parsedUrl <- .ParsedUrl(url)
	destfile <- file.path(cacheDir, gsub("^/", "", parsedUrl@path))
	destfile <- path.expand(destfile)
	synapseDownloadFileToDestination(url=url, checksum=checksum, destfile=destfile, opts=opts)
}

synapseDownloadFileToDestination  <- 
		function (url, checksum, destfile, curlHandle = getCurlHandle(), opts = .getCache("curlOpts"))
{
	## Download the file to a user-specified location
	if(file.exists(destfile)) {
		localFileChecksum <- md5sum(destfile)
		if(checksum == localFileChecksum) {
			# No need to download
			return(destfile)
		}
	}
	
	splits <- strsplit(destfile, .Platform$file.sep)
	downloadDir <- path.expand(paste(splits[[1]][-length(splits[[1]])], collapse=.Platform$file.sep))
	if(!file.exists(downloadDir)){
		dir.create(downloadDir, recursive=TRUE)
	}
	
	writeBin(getBinaryURL(url, curl = curlHandle, .opts = opts), con = destfile)
	tryCatch(
			.checkCurlResponse(curlHandle, paste(readLines(con=destfile, warn=FALSE), collapse='')), 
			error = function(ex){
				file.remove(destfile)
				stop(ex)
			}
	)
	return(destfile)
}

