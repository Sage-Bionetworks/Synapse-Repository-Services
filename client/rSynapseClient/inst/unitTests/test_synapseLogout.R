# TODO: Add comment
# 
# Author: furia
###############################################################################


.setUp <- function(){
	## make a copy of the old cache
	oldCache <- synapseClient:::.cache
	newCache <- new.env(parent=parent.env(oldCache))
	for(key in ls(oldCache))
		assign(key,get(key,envir=oldCache), envir=newCache)
	
	## re-define synapseLogin and synapseLogout
	mySynapseLogin <- function(username, password){
		synapseClient:::sessionToken(tempfile())
	}
	
	mySynapseLogout <- function(localOnly = FALSE){
		synapseClient:::sessionToken(NULL)
	}
	
	unloadNamespace("synapseClient")
	assignInNamespace("synapseLogin", mySynapseLogin, "synapseClient")
	assignInNamespace("synapseLogout", mySynapseLogout, "synapseClient")
	assignInNamespace(".cache", newCache, "synapseClient")
	attachNamespace("synapseClient")
	
	synapseClient:::.setCache("oldCache", oldCache)
	synapseClient:::sessionToken(NULL)
}

.tearDown <- function(){
	oldCache <- synapseClient:::.getCache("oldCache")
	unloadNamespace("synapseClient")
	assignInNamespace(".cache", oldCache, "synapseClient")
	attachNamespace("synapseClient")
}


unitTestSynapseLogout <- function(){
	synapseLogin("fakeUser", "fakePassword")
	
	checkTrue(synapseClient:::sessionToken() != "")
	
	synapseLogout()
	checkException(synapseClient:::sessionToken())	
}
