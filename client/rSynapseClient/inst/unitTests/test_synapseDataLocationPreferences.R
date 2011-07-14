.setUp <- function(){
	.setCache("untiTestSavedLocationPrefs", synapseDataLocationPreferences())
}

.tearDown <- function(){
	synapseDataLocationPreferences(.getCache("untiTestSavedLocationPrefs"))
	.deleteCache("untiTestSavedLocationPrefs")
}

unitTestHappyCase <- function() {
	
	synapseDataLocationPreferences(c('awss3'))
}

unitTestInvalidLocationPref <- function() {
	checkException(synapseDataLocationPreferences(c('invalid', 'awss3')))
}


