.setUp <- function(){
}

.tearDown <- function(){
}

unitTestHappyCase <- function() {
	synapseDataLocationPreferences(c('awss3'))
}

unitTestInvalidLocationPref <- function() {
	checkException(synapseDataLocationPreferences(c('invalid', 'awss3')))
}


