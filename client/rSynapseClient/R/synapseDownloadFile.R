synapseDownloadFile <- 
		function (url, destfile, method = curl, quiet = FALSE, mode = "w", cacheOK = TRUE, cacheDir = synapseCacheDir())
{
	destfile <- file.path(cacheDir, destfile)
	if (method == "curl") {
		extra <- if (quiet)
					"-s -S"
				else ""
		status <- system(paste("curl", extra, shQuote(url), "--create-dirs"," -k", " -o", 
						path.expand(destfile)))
		
	}else{
		stop("unsupported method:", method)
	}

	invisible(status)
}
