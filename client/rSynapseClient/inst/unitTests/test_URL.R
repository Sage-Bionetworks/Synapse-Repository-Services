.setUp <- function(){
	.setCache("testInput","https://s3.amazonaws.com/data01.sagebase.org/mskcc_prostate_cancer.phenotype.zip?Expires=1307658150&AWSAccessKeyId=AKIAI3BTGJG752CCJUVA&Signature=sN%2FNePyyQnkKwOWgTOxnLB5f42s%3D")
}

.tearDown <- function(){
	.deleteCache("testInput")
}

unitTestGetProtocol <- function(){
	url <- .ParsedUrl(.getCache("testInput"))
	checkEquals(url@protocol, "https")
}

unitTestGetHost <- function(){
	url <- .ParsedUrl(.getCache("testInput"))
	checkEquals(url@host, "s3.amazonaws.com")
}

unitTestGetPathPrefix <- function(){
	url <- .ParsedUrl(.getCache("testInput"))
	checkEquals(url@pathPrefix, "/data01.sagebase.org")
}

unitTestGetFileName <- function(){
	url <- .ParsedUrl(.getCache("testInput"))
	checkEquals(url@file, "mskcc_prostate_cancer.phenotype.zip")
}

unitTestGetQueryString <- function(){
	url <- .ParsedUrl(.getCache("testInput"))
	checkEquals(url@queryString, "Expires=1307658150&AWSAccessKeyId=AKIAI3BTGJG752CCJUVA&Signature=sN%2FNePyyQnkKwOWgTOxnLB5f42s%3D")
}

unitTestGetPath <- function(){
	url <- .ParsedUrl(.getCache("testInput"))
	checkEquals(url@path, "/data01.sagebase.org/mskcc_prostate_cancer.phenotype.zip")
}

unitTestGetHostWithPort <- function() {
	url <- .ParsedUrl('http://localhost:8080/services-authentication-0.6-SNAPSHOT/auth/v1')
	checkEquals(url@authority, 'localhost:8080')
	checkEquals(url@host, 'localhost')
	checkEquals(url@port, '8080')
	checkEquals(url@path, '/services-authentication-0.6-SNAPSHOT/auth/v1')
	checkEquals(url@file, 'v1')
	checkEquals(url@pathPrefix, '/services-authentication-0.6-SNAPSHOT/auth')
}


