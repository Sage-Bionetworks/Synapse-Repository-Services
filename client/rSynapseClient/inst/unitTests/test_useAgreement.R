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
		nrows <- synapseClient:::.getCache("eulaRows")
		if(is.null(nrows))
			return(nrows)
		if(nrows==0)
			return(data.frame())
		return(data.frame(matrix(nrow=nrows)))
	}
	
	myReadLine <- 
			function(prompt = "")
	{
		synapseClient:::.getCache("readlineResponse")
	}
	
	myGetParentEntity <- 
			function(entity)
	{
		return(new(Class="Dataset", properties=list(eulaId = "fakeEulaId")))
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
	synapseClient:::.setCache("oldCache", oldCache)
}

.tearDown <-
		function()
{
	oldCache <- synapseClient:::.getCache("oldCache")
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
	synapseClient:::.setCache("readlineResponse", "y")
	checkTrue(synapseClient:::.promptEulaAgreement(entity))
	
	synapseClient:::.setCache("readlineResponse", "Y")
	checkTrue(synapseClient:::.promptEulaAgreement(entity))
	
	synapseClient:::.setCache("readlineResponse", "Yes")
	checkTrue(synapseClient:::.promptEulaAgreement(entity))
	
	synapseClient:::.setCache("readlineResponse", "yes")
	checkTrue(synapseClient:::.promptEulaAgreement(entity))
	
	## test the various ways to reject the eula
	synapseClient:::.setCache("readlineResponse", "n")
	checkTrue(!synapseClient:::.promptEulaAgreement(entity))
	
	synapseClient:::.setCache("readlineResponse", "no")
	checkTrue(!synapseClient:::.promptEulaAgreement(entity))
	
	synapseClient:::.setCache("readlineResponse", "nO")
	checkTrue(!synapseClient:::.promptEulaAgreement(entity))
	
	synapseClient:::.setCache("readlineResponse", "NO")
	checkTrue(!synapseClient:::.promptEulaAgreement(entity))
	
	## test invalid responses
	synapseClient:::.setCache("readlineResponse", "yess")
	checkTrue(!synapseClient:::.promptEulaAgreement(entity))
	
	synapseClient:::.setCache("readlineResponse", "ye")
	checkTrue(!synapseClient:::.promptEulaAgreement(entity))
	
	synapseClient:::.setCache("readlineResponse", "noo")
	checkTrue(!synapseClient:::.promptEulaAgreement(entity))
	
	synapseClient:::.setCache("readlineResponse", "adfasf")
	checkTrue(!synapseClient:::.promptEulaAgreement(entity))
	
}


unitTestPromptEulaAgreementLayer <-
		function()
{
	entity <- new(Class="Layer")
	
	## test the various ways to accept the eula
	synapseClient:::.setCache("readlineResponse", "y")
	checkTrue(synapseClient:::.promptEulaAgreement(entity))
	
	synapseClient:::.setCache("readlineResponse", "Y")
	checkTrue(synapseClient:::.promptEulaAgreement(entity))
	
	synapseClient:::.setCache("readlineResponse", "Yes")
	checkTrue(synapseClient:::.promptEulaAgreement(entity))
	
	synapseClient:::.setCache("readlineResponse", "yes")
	checkTrue(synapseClient:::.promptEulaAgreement(entity))
	
	## test the various ways to reject the eula
	synapseClient:::.setCache("readlineResponse", "n")
	checkTrue(!synapseClient:::.promptEulaAgreement(entity))
	
	synapseClient:::.setCache("readlineResponse", "no")
	checkTrue(!synapseClient:::.promptEulaAgreement(entity))
	
	synapseClient:::.setCache("readlineResponse", "nO")
	checkTrue(!synapseClient:::.promptEulaAgreement(entity))
	
	synapseClient:::.setCache("readlineResponse", "NO")
	checkTrue(!synapseClient:::.promptEulaAgreement(entity))
	
	
	## test invalid responses
	synapseClient:::.setCache("readlineResponse", "yess")
	checkTrue(!synapseClient:::.promptEulaAgreement(entity))
	
	synapseClient:::.setCache("readlineResponse", "ye")
	checkTrue(!synapseClient:::.promptEulaAgreement(entity))
	
	synapseClient:::.setCache("readlineResponse", "noo")
	checkTrue(!synapseClient:::.promptEulaAgreement(entity))
	
	synapseClient:::.setCache("readlineResponse", "adfasf")
	checkTrue(!synapseClient:::.promptEulaAgreement(entity))
	
}

unitTestHasSignedEulaDataset <-
		function()
{
	entity <- new(Class="Dataset")
	propertyValue(entity, "eulaId") <- "fakeEulaId"
	
	## test NULL response
	synapseClient:::.setCache("eulaRows", NULL)
	checkTrue(!synapseClient:::hasSignedEula(entity))
	
	## test 0 rows returned
	synapseClient:::.setCache("eulaRows", 0)
	checkTrue(!synapseClient:::hasSignedEula(entity))
	
	## test 0 rows returned
	synapseClient:::.setCache("eulaRows", 1)
	checkTrue(synapseClient:::hasSignedEula(entity))
	
	## test 0 rows returned
	synapseClient:::.setCache("eulaRows", 2)
	checkTrue(synapseClient:::hasSignedEula(entity))
	
}

unitTestHasSignedEulaLayer <-
		function()
{
	entity <- new(Class="Layer")
	
	## test NULL response
	synapseClient:::.setCache("eulaRows", NULL)
	checkTrue(!synapseClient:::hasSignedEula(entity))
	
	## test 0 rows returned
	synapseClient:::.setCache("eulaRows", 0)
	checkTrue(!synapseClient:::hasSignedEula(entity))
	
	## test 0 rows returned
	synapseClient:::.setCache("eulaRows", 1)
	checkTrue(synapseClient:::hasSignedEula(entity))
	
	## test 0 rows returned
	synapseClient:::.setCache("eulaRows", 2)
	checkTrue(synapseClient:::hasSignedEula(entity))
	
}

unitTestPromptSignEula <-
		function()
{
	## test the various ways to accept the eula
	synapseClient:::.setCache("readlineResponse", "y")
	checkTrue(synapseClient:::.promptSignEula())
	
	synapseClient:::.setCache("readlineResponse", "Y")
	checkTrue(synapseClient:::.promptSignEula())
	
	synapseClient:::.setCache("readlineResponse", "Yes")
	checkTrue(synapseClient:::.promptSignEula())
	
	synapseClient:::.setCache("readlineResponse", "yes")
	checkTrue(synapseClient:::.promptSignEula())
	
	## test the various ways to reject the eula
	synapseClient:::.setCache("readlineResponse", "n")
	checkTrue(!synapseClient:::.promptSignEula())
	
	synapseClient:::.setCache("readlineResponse", "no")
	checkTrue(!synapseClient:::.promptSignEula())
	
	synapseClient:::.setCache("readlineResponse", "nO")
	checkTrue(!synapseClient:::.promptSignEula())
	
	synapseClient:::.setCache("readlineResponse", "NO")
	checkTrue(!synapseClient:::.promptSignEula())
	
	## test invalid responses
	synapseClient:::.setCache("readlineResponse", "yess")
	checkTrue(!synapseClient:::.promptSignEula())
	
	synapseClient:::.setCache("readlineResponse", "ye")
	checkTrue(!synapseClient:::.promptSignEula())
	
	synapseClient:::.setCache("readlineResponse", "noo")
	checkTrue(!synapseClient:::.promptSignEula())
	
	synapseClient:::.setCache("readlineResponse", "adfasf")
	checkTrue(!synapseClient:::.promptSignEula())
	
}

unitTestSignEula <- 
		function()
{
	checkException(synapseClient:::.signEula(new(Class="SynapseEntity")))
}

unitTestNoEulaId <-
		function()
{
	entity <- new(Class="Dataset")
	checkTrue(synapseClient:::hasSignedEula(entity))
}