.setUp <-
		function()
{
	## make a copy of the old cache
	oldCache <- synapseClient:::.cache
	newCache <- new.env(parent=parent.env(oldCache))
	for(key in ls(oldCache))
		assign(key,get(key,envir=oldCache), envir=newCache)
	
	myGetURL <- 
			function(uri, postfields, customrequest, httpheader, curl, .opts)
	{
		entity <- as.list(fromJSON(entity))
		if(entity$sessionToken == .getCache("validToken")){
			## return the response for a valid sessionToken and set the 
			## curl handle HTTP response accordingly
		}else if(entity$sessionToken == .getCache("inValidToken")){
			## return the response for a valid sessionToken and set the 
			## curl handle HTTP response accordingly
		}
	}
	
	## attach the overridden methods
	unloadNamespace('synapseClient')
	unloadNamespace("RCurl")
	assignInNamespace("getURL", myGetURL, "RCurl")
	assignInNamespace(".cache", newCache, "synapseClient")
	attachNamespace("synapseClient")
	
	.setCache("oldCache", oldCache)
	.setCache("validToken", "thisIsAFakeValidSessionToken")
	.setCache("inValidToken", "thisIsAFakeInValidSessionToken")
}

.tearDown <-
		function()
{
	oldCache <- .getCache("oldCache")
	
	## put back the original function definitions and cache
	unloadNamespace('synapseClient')
	unloadNamespace("RCurl")
	assignInNamespace(".cache", oldCache, "synapseClient")
	attachNamespace("synapseClient")
}

unitTestRefresh <-
		function()
{
	
}