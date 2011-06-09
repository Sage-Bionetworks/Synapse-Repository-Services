synapseDownloadFile  <- 
		function (url, destfile, curlHandle = getCurlHandle(), cacheDir = synapseCacheDir())
{
	destfile <- file.path(cacheDir, destfile)
	splits <- strsplit(destfile, .Platform$file.sep)
	downloadDir <- paste(splits[[1]][-length(splits[[1]])], collapse=.Platform$file.sep)
	if(!file.exists(downloadDir)){
		dir.create(downloadDir, recursive=TRUE)
	}
	writeBin(getBinaryURL(url, curl = curlHandle), con = destfile)
	tryCatch(
			checkCurlResponse(curlHandle, paste(readLines(con=destfile, warn=FALSE), collapse='')), 
			error = function(ex){
				file.remove(destfile)
				stop(ex)
			}
	)
}
