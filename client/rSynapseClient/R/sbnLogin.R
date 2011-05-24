sbnLogin <- function(username, password){
	#constants
	kHeader <-  c('Content-Type'="application/json",Accept="application/json")
	kService <- "auth/v1/session"
	#end constants
	
	#build the header containing username and password
	httpBody <- paste('{\"userId\":\"', 
			username, 
			'\", \"password\":\"', 
			password, '\"}', 
			sep='')
	
	#build the uri
	curl.handle <- getCurlHandle()
	uri <- paste(sbnSslHostName(), kService, sep='/')
	
	opts <- list(ssl.verifypeer = FALSE)
	#Login and check for success
	response <- getURL(url=uri, postfields=httpBody, httpheader=kHeader, curl=curl.handle, .opts=opts)
	checkCurlResponse(curl.handle, response)
	response <- fromJSON(response)
	
	#cache the session token and print out welcome message
	sessionToken(response$sessionToken)
	cat(paste("Welcome ", response$displayName, "!\n", sep=""))
}
