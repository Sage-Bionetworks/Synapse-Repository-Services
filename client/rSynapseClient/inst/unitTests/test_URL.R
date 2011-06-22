.setUp <- function(){
	.setCache("exURL","https://s3.amazonaws.com/data01.sagebase.org/mskcc_prostate_cancer.phenotype.zip?Expires=1307658150&AWSAccessKeyId=AKIAI3BTGJG752CCJUVA&Signature=sN%2FNePyyQnkKwOWgTOxnLB5f42s%3D")
}

.tearDown <- function(){
	.deleteCache("exURL")
}

unitTestGetProtocol <- function(){
	url <- URL(.getCache("exURL"))
	checkEquals(url@protocol, "https")
}

unitTestGetHost <- function(){
	url <- URL(.getCache("exURL"))
	checkEquals(url@host, "s3.amazonaws.com")
}

unitTestGetPathPrefix <- function(){
	url <- URL(.getCache("exURL"))
	checkEquals(url@pathPrefix, "/data01.sagebase.org")
}

unitTestGetFileName <- function(){
	url <- URL(.getCache("exURL"))
	checkEquals(url@file, "mskcc_prostate_cancer.phenotype.zip")
}

unitTestGetQueryString <- function(){
	url <- URL(.getCache("exURL"))
	checkEquals(url@queryString, "Expires=1307658150&AWSAccessKeyId=AKIAI3BTGJG752CCJUVA&Signature=sN%2FNePyyQnkKwOWgTOxnLB5f42s%3D")
}

unitTestGetPath <- function(){
	url <- URL(.getCache("exURL"))
	checkEquals(url@path, "/data01.sagebase.org/mskcc_prostate_cancer.phenotype.zip")
}

unitTestGetHostWithPort <- function() {
	url <- URL('http://localhost:8080/services-authentication-0.5-SNAPSHOT/auth/v1')
	checkEquals(url@authority, 'localhost:8080')
	checkEquals(url@host, 'localhost')
	checkEquals(url@port, '8080')
	checkEquals(url@path, '/services-authentication-0.5-SNAPSHOT/auth/v1')
	checkEquals(url@file, 'v1')
	checkEquals(url@pathPrefix, '/services-authentication-0.5-SNAPSHOT/auth')
}


