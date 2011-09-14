# Tests for signing use agreements
# 
# Author: Matt Furia
###############################################################################

.setUp <- 
		function()
{
	## make a copy of the old cache
	oldCache <- synapseClient:::.cache
	newCache <- new.env(parent=parent.env(oldCache))
	for(key in ls(oldCache))
		assign(key,get(key,envir=oldCache), envir=newCache)
	
	mySynapseQuery <-
			function(queryString)
	{
		nrows <- .getCache("eulaRows")
		if(is.null(nrows))
			return(nrows)
		if(nrows==0)
			return(data.frame())
		return(data.frame(matrix(nrow=nrows)))
	}
	
	myReadLine <- 
			function(prompt = "")
	{
		.getCache("readlineResponse")
	}
	
	myGetParentEntity <- 
			function(entity)
	{
		return(new(Class="Dataset"))
	}
	
	myGetEntity <- 
			function(entity)
	{
		entity <- new(Class="Eula")
		propertyValue(entity, "agreement") <- "This is fake Eula text."
		return(entity)
	}
	
	myFile.show <-
			function(..., header = rep("", nfiles), title = "R Information", 
					delete.file = FALSE, pager = getOption("pager"), encoding = "")
	{
		
	}
	
	attr(myReadLine, "oldFcn") <- readline
	attr(myFile.show, "oldFcn") <- file.show
	assignInNamespace("readline", myReadLine, "base")
	assignInNamespace("file.show", myFile.show, "base")
	
	unloadNamespace("synapseClient")
	assignInNamespace(".cache", newCache, "synapseClient")
	assignInNamespace("synapseQuery", mySynapseQuery, "synapseClient")
	assignInNamespace("getParentEntity", myGetParentEntity, "synapseClient")
	assignInNamespace("getEntity", myGetEntity, "synapseClient")
	attachNamespace("synapseClient")
	.setCache("oldCache", oldCache)
}

.tearDown <-
		function()
{
	oldCache <- .getCache("oldCache")
	# put back the overridden functions and original cache
	assignInNamespace("readline", attr(readline, "oldFcn"), "base")
	assignInNamespace("file.show", attr(file.show, "oldFcn"), "base")
	
	unloadNamespace("synapseClient")
	assignInNamespace(".cache", oldCache, "synapseClient")
	attachNamespace("synapseClient")
	
	unloadNamespace("synapseClient")
	assignInNamespace(".cache", oldCache, "synapseClient")
	attachNamespace("synapseClient")
}

unitTestPromptEulaAgreementDataset <-
		function()
{
	entity <- new(Class="Dataset")
	
	## test the various ways to accept the eula
	.setCache("readlineResponse", "y")
	checkTrue(.promptEulaAgreement(entity))
	
	.setCache("readlineResponse", "Y")
	checkTrue(.promptEulaAgreement(entity))
	
	.setCache("readlineResponse", "Yes")
	checkTrue(.promptEulaAgreement(entity))
	
	.setCache("readlineResponse", "yes")
	checkTrue(.promptEulaAgreement(entity))
	
	## test the various ways to reject the eula
	.setCache("readlineResponse", "n")
	checkTrue(!.promptEulaAgreement(entity))
	
	.setCache("readlineResponse", "no")
	checkTrue(!.promptEulaAgreement(entity))
	
	.setCache("readlineResponse", "nO")
	checkTrue(!.promptEulaAgreement(entity))
	
	.setCache("readlineResponse", "NO")
	checkTrue(!.promptEulaAgreement(entity))
	
	## test the various ways to cancel
	.setCache("readlineResponse", "cancel")
	checkTrue(!.promptEulaAgreement(entity))
	
	.setCache("readlineResponse", "c")
	checkTrue(!.promptEulaAgreement(entity))
	
	.setCache("readlineResponse", "Cancel")
	checkTrue(!.promptEulaAgreement(entity))
	
	.setCache("readlineResponse", "CanCel")
	checkTrue(!.promptEulaAgreement(entity))
	
	## test invalid responses
	.setCache("readlineResponse", "yess")
	checkTrue(!.promptEulaAgreement(entity))
	
	.setCache("readlineResponse", "ye")
	checkTrue(!.promptEulaAgreement(entity))
	
	.setCache("readlineResponse", "noo")
	checkTrue(!.promptEulaAgreement(entity))
	
	.setCache("readlineResponse", "adfasf")
	checkTrue(!.promptEulaAgreement(entity))
	
}


unitTestPromptEulaAgreementLayer <-
		function()
{
	entity <- new(Class="Layer")
	
	## test the various ways to accept the eula
	.setCache("readlineResponse", "y")
	checkTrue(.promptEulaAgreement(entity))
	
	.setCache("readlineResponse", "Y")
	checkTrue(.promptEulaAgreement(entity))
	
	.setCache("readlineResponse", "Yes")
	checkTrue(.promptEulaAgreement(entity))
	
	.setCache("readlineResponse", "yes")
	checkTrue(.promptEulaAgreement(entity))
	
	## test the various ways to reject the eula
	.setCache("readlineResponse", "n")
	checkTrue(!.promptEulaAgreement(entity))
	
	.setCache("readlineResponse", "no")
	checkTrue(!.promptEulaAgreement(entity))
	
	.setCache("readlineResponse", "nO")
	checkTrue(!.promptEulaAgreement(entity))
	
	.setCache("readlineResponse", "NO")
	checkTrue(!.promptEulaAgreement(entity))
	
	## test the various ways to cancel
	.setCache("readlineResponse", "cancel")
	checkTrue(!.promptEulaAgreement(entity))
	
	.setCache("readlineResponse", "c")
	checkTrue(!.promptEulaAgreement(entity))
	
	.setCache("readlineResponse", "Cancel")
	checkTrue(!.promptEulaAgreement(entity))
	
	.setCache("readlineResponse", "CanCel")
	checkTrue(!.promptEulaAgreement(entity))
	
	## test invalid responses
	.setCache("readlineResponse", "yess")
	checkTrue(!.promptEulaAgreement(entity))
	
	.setCache("readlineResponse", "ye")
	checkTrue(!.promptEulaAgreement(entity))
	
	.setCache("readlineResponse", "noo")
	checkTrue(!.promptEulaAgreement(entity))
	
	.setCache("readlineResponse", "adfasf")
	checkTrue(!.promptEulaAgreement(entity))
	
}

unitTestHasSignedEulaDataset <-
		function()
{
	entity <- new(Class="Dataset")
	
	## test NULL response
	.setCache("eulaRows", NULL)
	checkTrue(!hasSignedEula(entity))
	
	## test 0 rows returned
	.setCache("eulaRows", 0)
	checkTrue(!hasSignedEula(entity))
	
	## test 0 rows returned
	.setCache("eulaRows", 1)
	checkTrue(hasSignedEula(entity))
	
	## test 0 rows returned
	.setCache("eulaRows", 2)
	checkTrue(hasSignedEula(entity))
	
}

unitTestHasSignedEulaLayer <-
		function()
{
	entity <- new(Class="Layer")
	
	## test NULL response
	.setCache("eulaRows", NULL)
	checkTrue(!hasSignedEula(entity))
	
	## test 0 rows returned
	.setCache("eulaRows", 0)
	checkTrue(!hasSignedEula(entity))
	
	## test 0 rows returned
	.setCache("eulaRows", 1)
	checkTrue(hasSignedEula(entity))
	
	## test 0 rows returned
	.setCache("eulaRows", 2)
	checkTrue(hasSignedEula(entity))
	
}

unitTestPromptSignEula <-
		function()
{
	## test the various ways to accept the eula
	.setCache("readlineResponse", "y")
	checkTrue(.promptSignEula())
	
	.setCache("readlineResponse", "Y")
	checkTrue(.promptSignEula())
	
	.setCache("readlineResponse", "Yes")
	checkTrue(.promptSignEula())
	
	.setCache("readlineResponse", "yes")
	checkTrue(.promptSignEula())
	
	## test the various ways to reject the eula
	.setCache("readlineResponse", "n")
	checkTrue(!.promptSignEula())
	
	.setCache("readlineResponse", "no")
	checkTrue(!.promptSignEula())
	
	.setCache("readlineResponse", "nO")
	checkTrue(!.promptSignEula())
	
	.setCache("readlineResponse", "NO")
	checkTrue(!.promptSignEula())
	
	## test the various ways to cancel
	.setCache("readlineResponse", "cancel")
	checkTrue(!.promptSignEula())
	
	.setCache("readlineResponse", "c")
	checkTrue(!.promptSignEula())
	
	.setCache("readlineResponse", "Cancel")
	checkTrue(!.promptSignEula())
	
	.setCache("readlineResponse", "CanCel")
	checkTrue(!.promptSignEula())
	
	## test invalid responses
	.setCache("readlineResponse", "yess")
	checkTrue(!.promptSignEula())
	
	.setCache("readlineResponse", "ye")
	checkTrue(!.promptSignEula())
	
	.setCache("readlineResponse", "noo")
	checkTrue(!.promptSignEula())
	
	.setCache("readlineResponse", "adfasf")
	checkTrue(!.promptSignEula())
	
}

unitTestSignEula <- 
		function()
{
	checkException(.signEula(new(Class="SynapseEntity")))
}
