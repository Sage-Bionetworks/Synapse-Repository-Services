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
		function (url, srcfile, checksum, contentType = 'application/binary', method = "curl", quiet = FALSE, mode = "w", cacheOK = TRUE)
{
	header = c("Content-Type"=contentType,
			"x-amz-acl"= "bucket-owner-full-control",
			"Content-MD5" = .hexMD5ToBase64MD5(checksum)
	)
	if (method == "curl") {	
		.curlReaderUpload(url=url, srcfile=srcfile, header=header)
	}else{
		stop("unsupported method:", method)
	}
}