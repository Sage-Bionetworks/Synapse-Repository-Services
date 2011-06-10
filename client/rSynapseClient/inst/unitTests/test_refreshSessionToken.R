.setUp <-
		function()
{
	.setCache("validToken", "thisIsAFakeValidSessionToken")
	.setCache("inValidToken", "thisIsAFakeInValidSessionToken")
	myGetURL <- 
			function(uri, postfields, customrequest, httpheader, curl, .opts)
	{
		entity <- fromJSON(entity)
		if(entity$sessionToken == .getCache("validToken")){
			## return the response for a valid session token and set the 
			## curl handle HTTP response accordingly
		}else if(entity$sessionToken == .getCache("inValidToken")){
			## return the response for a valid session token and set the 
			## curl handle HTTP response accordingly
		}
	}
	
	attr(myGetURL, origFcn) <- RCurl:::getURL
	detach('package:RCurl', force = TRUE)
	assignInNamespace("getURL", myGetURL, "RCurl")
	library(RCurl, quietly = TRUE)
}

.tearDown <-
		function()
{
	.deleteCache("validToken")
	.deleteCache("inValidToken")
}

testRefresh <-
		function()
{
	
}