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
			function(url, destfile=tempfile(), curlHandle = getCurlHandle(), writeFunction=synapseClient:::.getCache('curlWriter'), opts = synapseClient:::.getCache("curlOpts"))
	{
		if(url == synapseClient:::.getCache("goodURL")){
			filePath <- synapseClient:::.getCache("goodFilePath")
		}else if(url == synapseClient:::.getCache("badURL")){
			filePath <- synapseClient:::.getCache("badFilePath")
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
	synapseClient:::.setCache("goodFilePath", goodFilePath)
	synapseClient:::.setCache("badFilePath", badFilePath)
	synapseClient:::.setCache("checksum", as.character(tools::md5sum(goodFilePath)))
	synapseClient:::.setCache("oldCache", oldCache)
	synapseClient:::.setCache("goodURL", "http://fakeUrl.goodChecksum")
	synapseClient:::.setCache("badURL", "http://fakeUrl.badChecksum")
}

.tearDown <-
		function()
{
	oldCache <- synapseClient:::.getCache("oldCache")
	# put back the overridden functions and original cache
	unloadNamespace("synapseClient")
	assignInNamespace(".cache", oldCache, "synapseClient")
	attachNamespace("synapseClient")
}

unitTestChecksumMatches <-
		function()
{
	file <- synapseClient:::synapseDownloadFile(synapseClient:::.getCache("goodURL"),synapseClient:::.getCache("checksum"))
	checkEquals(as.character(tools::md5sum(file)), synapseClient:::.getCache("checksum"))
}

unitTestBadChecksum <-
		function()
{
	
	file <- synapseClient:::.curlWriterDownload(synapseClient:::.getCache('badURL'))
	checkEquals(as.character(tools::md5sum(file)), as.character(tools::md5sum(synapseClient:::.getCache("badFilePath"))))
	checkTrue(as.character(tools::md5sum(synapseClient:::.getCache("goodFilePath"))) != as.character(tools::md5sum(synapseClient:::.getCache("badFilePath"))))
	checkException(synapseClient:::synapseDownloadFile(synapseClient:::.getCache("badURL"), synapseClient:::.getCache("checksum")))
}
