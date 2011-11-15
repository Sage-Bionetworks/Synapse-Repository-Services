# TODO: Add comment
# 
# Author: furia
###############################################################################


.curlReaderOpen <-
		function(filename)
{
	if (!is.character(filename) || 1L != length(filename))
		stop("'filename' must be character(1)")
	if (!file.exists(filename) || file.info(filename)$isdir)
		stop("'filename' does not exist or is a directory")
	
	.Call("reader_open", filename)
}

.curlReaderClose <-
		function(ext)
{
	.Call("reader_close", ext)
}

.curlReaderUpload <-
		function(url, srcfile, header, curlHandle = getCurlHandle(), readFunction=.getCache('curlReader'), opts = .getCache("curlOpts"))
{
	parsedUrl <- .ParsedUrl(url)
	if(tolower(parsedUrl@protocol) == "file"){
		if(file.exists(parsedUrl@path))
			file.remove(parsedUrl@path)
		file.create(parsedUrl@path)
	}
	ext <- .curlReaderOpen(srcfile)
	on.exit(.curlReaderClose(ext))
	opts$noprogress <- 0L
	opts$put <- 1L
	opts$infilesize <- file.info(srcfile)$size
	if(missing(header)){
		response <- curlPerform(URL=url, readfunction=readFunction,readdata=ext, .opts = opts)
	}else{
		response <- curlPerform(URL=url, readfunction=readFunction,readdata=ext, httpHeader=header, .opts = opts)
	}
	.checkCurlResponse(curlHandle, response)
}
	