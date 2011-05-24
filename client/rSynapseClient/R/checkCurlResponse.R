setGeneric(
		name="checkCurlResponse",
		def=function(object, response){
			standardGeneric("checkCurlResponse")
		}
)

setMethod("checkCurlResponse", "CURLHandle",
		function(object, response){
			response.code <- getCurlInfo(object)$response.code
			if(response.code < 200 | response.code >= 300 ){
				stop(paste(paste("HTTP Error:", response.code), response, sep = '\n'))
			}
		}
)
