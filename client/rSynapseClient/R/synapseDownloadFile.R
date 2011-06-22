synapseDownloadFile  <- 
		function (url, destfile, checksum, curlHandle = getCurlHandle(), cacheDir = synapseCacheDir(), opts = .getCache("curlOpts"))
{
	destfile <- path.expand(destfile)
	splits <- strsplit(destfile, .Platform$file.sep)
	downloadDir <- path.expand(paste(splits[[1]][-length(splits[[1]])], collapse=.Platform$file.sep))
	
	##TODO use the checksum
	if(!file.exists(downloadDir)){
		dir.create(downloadDir, recursive=TRUE)
	}
	writeBin(getBinaryURL(url, curl = curlHandle, .opts = opts), con = destfile)
	tryCatch(
			checkCurlResponse(curlHandle, paste(readLines(con=destfile, warn=FALSE), collapse='')), 
			error = function(ex){
				file.remove(destfile)
				stop(ex)
			}
	)
}

