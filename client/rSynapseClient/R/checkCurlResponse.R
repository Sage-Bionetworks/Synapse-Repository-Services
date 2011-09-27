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
		if(!missing(response)) {
			if(grepl('The AWS Access Key Id you provided does not exist in our records.', response)) {
				# The propagation delay for new IAM users is anywhere from 5 to 60 seconds
				stop(paste("Try your request again, but if it doesn't work within 2 minutes, contact the Synapse team for help.", 
								message, response, sep = '\n'))
			}
			else {
				stop(paste(message, response, sep = '\n'))
			}
		} else{
			stop(message)
		}
	}
}