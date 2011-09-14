# Test handling of md5sum checks after download
# 
# Author: Matt Furia
###############################################################################


.setUp <- 
		function()
{
	## make a copy of the old cache
	oldCache <- synapseClient:::.cache
	newCache <- new.env(parent=parent.env(oldCache))
	for(key in ls(oldCache))
		assign(key,get(key,envir=oldCache), envir=newCache)
	
	badFilePath <- tempfile()
	goodFilePath <- tempfile()
	d <- diag(nrow=10, ncol=10)
	save(d, file = goodFilePath)
	write.table(d, file = badFilePath)
	
	## override synapseDownloadFile
	myCurlWriterDownload <-
			function(url, destfile=tempfile(), curlHandle = getCurlHandle(), writeFunction=.getCache('curlWriter'), opts = .getCache("curlOpts"))
	{
		if(url == .getCache("goodURL")){
			filePath <- .getCache("goodFilePath")
		}else if(url == .getCache("badURL")){
			filePath <- .getCache("badFilePath")
		}else{
			stop("invalid URL")
		}
		file.copy(filePath,destfile)
		destfile	
	}
	unloadNamespace('synapseClient')
	assignInNamespace(".curlWriterDownload", myCurlWriterDownload, "synapseClient")
	assignInNamespace(".cache", newCache, "synapseClient")
	attachNamespace("synapseClient")
	.setCache("goodFilePath", goodFilePath)
	.setCache("badFilePath", badFilePath)
	.setCache("checksum", as.character(tools::md5sum(goodFilePath)))
	.setCache("oldCache", oldCache)
	.setCache("goodURL", "http://fakeUrl.goodChecksum")
	.setCache("badURL", "http://fakeUrl.badChecksum")
}

.tearDown <-
		function()
{
	oldCache <- .getCache("oldCache")
	# put back the overridden functions and original cache
	unloadNamespace("synapseClient")
	assignInNamespace(".cache", oldCache, "synapseClient")
	attachNamespace("synapseClient")
}

unitTestChecksumMatches <-
		function()
{
	file <- synapseDownloadFile(.getCache("goodURL"), .getCache("checksum"))
	checkEquals(as.character(tools::md5sum(file)), .getCache("checksum"))
}

#unitTestBadChecksum <-
#		function()
#{
#	
#	file <- .curlWriterDownload(.getCache('badURL'))
#	checkEquals(as.character(tools::md5sum(file)), as.character(tools::md5sum(.getCache("badFilePath"))))
#	checkTrue(as.character(tools::md5sum(.getCache("goodFilePath"))) != as.character(tools::md5sum(.getCache("badFilePath"))))
#	checkException(synapseDownloadFile(.getCache("badURL"), .getCache("checksum")))
#}