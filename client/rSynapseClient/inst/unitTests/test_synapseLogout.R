# TODO: Add comment
# 
# Author: furia
###############################################################################


.setUp <- function(){
	.setCache("oldToken",.getCache("sessionToken"))
	sessionToken(NULL)
	
	## re-define synapseLogin and synapseLogout
	mySynapseLogin <- function(username, password){
		sessionToken(tempfile())
	}
	
	mySynapseLogout <- function(localOnly = FALSE){
		fcn <- attr(synapseLogout, "origFcn")
		fcn(localOnly=TRUE)
	}
	
	attr(mySynapseLogin, "origFcn") <- synapseClient:::synapseLogin
	attr(mySynapseLogout, "origFcn") <- synapseClient:::synapseLogout
	detach('package:synapseClient', force = TRUE)
	assignInNamespace("synapseLogin", mySynapseLogin, "synapseClient")
	assignInNamespace("synapseLogout", mySynapseLogout, "synapseClient")
	library(synapseClient, quietly = TRUE)
}

.tearDown <- function(){
	sessionToken(.getCache("oldToken"))
	.deleteCache("oldToken")
	
	detach('package:synapseClient', force = TRUE)
	assignInNamespace("synapseLogin", attr(synapseClient:::synapseLogin, "origFcn"), "synapseClient")
	assignInNamespace("synapseLogout", attr(synapseClient:::synapseLogout, "origFcn"), "synapseClient")
	library(synapseClient)
}


unitTestSynapseLogout <- function(){
	synapseLogin("fakeUser", "fakePassword")
	
	checkTrue(sessionToken() != "")
	
	synapseLogout()
	checkException(sessionToken())	
}
