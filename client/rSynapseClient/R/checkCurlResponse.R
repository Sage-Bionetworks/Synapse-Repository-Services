#setGeneric(
#		name=".checkCurlResponse",
#		def=function(object, response){
#			standardGeneric(".checkCurlResponse")
#		}
#)
#
#setMethod(".checkCurlResponse", "CURLHandle",
#		function(object, response){
#			info <- getCurlInfo(object)
#			if(info$responsecode != 0 & (info$response.code < 200 || info$response.code >= 300)){
#				message <- paste("HTTP Error:", info$response.code, "for request", info$effective.url)
#				if(!missing(response)){
#					stop(paste(message, response, sep = '\n'))
#				} else{
#					stop(message)
#				}
#			}
#		}
#)

.checkCurlResponse <- function(object, response){
	if(class(object) != "CURLHandle") stop("invalid curl handle")
	info <- getCurlInfo(object)
	if(info$response.code != 0 & (info$response.code < 200 || info$response.code >= 300)){
		message <- paste("HTTP Error:", info$response.code, "for request", info$effective.url)
		if(!missing(response)){
			stop(paste(message, response, sep = '\n'))
		} else{
			stop(message)
		}
	}
}