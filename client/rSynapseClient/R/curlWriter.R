.curlWriterOpen <-
		function(filename)
{
	if (!is.character(filename) || 1L != length(filename))
		stop("'filename' must be character(1)")
	dir <- dirname(filename)
	if (!file.exists(dir) || !file.info(dir)$isdir)
		stop("'dirname(filename)' does not exist or is not a directory")
	filename <- file.path(normalizePath(dir), basename(filename))
	if (file.exists(filename))
		stop("'filename' must not already exist")
	
	.Call("writer_open", filename)
}

.curlWriterClose <-
		function(ext)
{
	.Call("writer_close", ext)
}

.curlWriterDownload <-
		function(url, destfile=tempfile(), curlHandle = getCurlHandle(), writeFunction=.getCache('curlWriter'), opts = .getCache("curlOpts"))
{
	ext <- .curlWriterOpen(destfile)
	on.exit(.curlWriterClose(ext))
	response <- curlPerform(URL=url, writefunction=writeFunction,
			writedata=ext, .opts = opts)
	.checkCurlResponse(curlHandle, response)
	return(destfile)
}
