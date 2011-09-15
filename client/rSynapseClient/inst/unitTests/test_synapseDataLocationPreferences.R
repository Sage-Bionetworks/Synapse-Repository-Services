.setUp <- function(){
	synapseClient:::.setCache("untiTestSavedLocationPrefs", synapseClient:::synapseDataLocationPreferences())
}

.tearDown <- function(){
	synapseClient:::synapseDataLocationPreferences(synapseClient:::.getCache("untiTestSavedLocationPrefs"))
	synapseClient:::.deleteCache("untiTestSavedLocationPrefs")
}

unitTestHappyCase <- function() {
	
	synapseClient:::synapseDataLocationPreferences(c('awss3'))
}

unitTestInvalidLocationPref <- function() {
	checkException(synapseClient:::synapseDataLocationPreferences(c('invalid', 'awss3')))
}


