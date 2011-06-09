library(tools)

hexMD5ToBase64MD5 <- function(checksumHex) {
	checksumBytes <- raw(16)
	for (i in 1:16)
	{
		hex <- paste('0x', substr(checksumHex, ((i*2)-1), (i*2)), sep="")
		checksumBytes[i] <- as.raw(hex)
	}
	checksumBase64 <- base64Encode(checksumBytes)
	checksumBase64[[1]]
}

unitTestMd5hexToBase64 <- function() {
	checkEquals('BP5xIZ38aXAq1sYBz2iDbw==', hexMD5ToBase64MD5('04fe71219dfc69702ad6c601cf68836f')) 
}


