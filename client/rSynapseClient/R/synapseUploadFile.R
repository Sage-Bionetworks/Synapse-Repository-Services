.hexMD5ToBase64MD5 <- function(checksumHex) {
	checksumBytes <- raw(16)
	for (i in 1:16)
	{
		hex <- paste('0x', substr(checksumHex, ((i*2)-1), (i*2)), sep="")
		checksumBytes[i] <- as.raw(hex)
	}
	checksumBase64 <- base64Encode(checksumBytes)
	checksumBase64[[1]]
}

synapseUploadFile <- 
		function (url, srcfile, checksum, method = "curl", quiet = FALSE, mode = "w", cacheOK = TRUE)
{
	
	if (method == "curl") {
		extra <- if (quiet)
					" -s -S "
				else if(.getCache("debug")) {
					"-v"
				}
				else ""
		status <- system(paste("curl", extra, " -k ", 
						" -H Content-Type:application/binary",
						" -H Content-MD5:", .hexMD5ToBase64MD5(checksum),
						" -H x-amz-acl:bucket-owner-full-control",
						" --data-binary ", "@", path.expand(srcfile),
						" -X PUT ",
						shQuote(url), sep=""))
		
	}else{
		stop("unsupported method:", method)
	}
	
	invisible(status)
}