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
		sessionToken(tempfile())
	}
	
	mySynapseLogout <- function(localOnly = FALSE){
		sessionToken(NULL)
	}
	
	unloadNamespace("synapseClient")
	assignInNamespace("synapseLogin", mySynapseLogin, "synapseClient")
	assignInNamespace("synapseLogout", mySynapseLogout, "synapseClient")
	assignInNamespace(".cache", newCache, "synapseClient")
	attachNamespace("synapseClient")
	
	.setCache("oldCache", oldCache)
	sessionToken(NULL)
}

.tearDown <- function(){
	oldCache <- .getCache("oldCache")
	unloadNamespace("synapseClient")
	assignInNamespace(".cache", oldCache, "synapseClient")
	attachNamespace("synapseClient")
}


unitTestSynapseLogout <- function(){
	synapseLogin("fakeUser", "fakePassword")
	
	checkTrue(sessionToken() != "")
	
	synapseLogout()
	checkException(sessionToken())	
}
