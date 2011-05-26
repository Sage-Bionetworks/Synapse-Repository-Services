synapseGet <- function(uri, curl.handle=getCurlHandle(), anonymous = TRUE){

  if(!is.character(uri)){
		stop("a uri must be supplied of R type character")
	}
    
	# Constants
	kPath <- "/repo/v1"
	kHeader <- c(Accept = "application/json")
	# end constants
	
	uri <- paste(sbnHostName(), kPath, uri, sep="")
	
	# Prepare the header. If not an anonymous request, stuff the
	# session token into the header
	header <- kHeader
	if(!anonymous){
		header <- c(header, sessionToken = sessionToken())
	}
	
	# Submit request and check response code
  d = debugGatherer()
  response <- getURL(uri, debugfunction=d$update, verbose = TRUE, httpheader = header, curl = curl.handle)
  d$value()
	checkCurlResponse(curl.handle, response)
	
	# Parse response and prepare return value
	result <- fromJSON(response)

	return(result)
}

