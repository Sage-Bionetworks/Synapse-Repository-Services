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
		function (url, destfile, checksum, curlHandle = getCurlHandle(), opts = .getCache("curlOpts"))
{
	## Download the file to a user-specified location
	## if checksum is missing, don't check local file before 
	## download
	if(file.exists(destfile) & !missing(checksum)) {
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
	
	## download to temp file first so that the existing local file (if there is one) is left in place
	## if the download fails
	tmpFile <- tempfile()
	tryCatch(
			.curlWriterDownload(url=url, destfile=tmpFile),
			error = function(ex){
				file.remove(tmpFile)
				stop(ex)
			}
	)
	file.copy(tmpFile, destfile)
	file.remove(tmpFile)
	return(destfile)
}
