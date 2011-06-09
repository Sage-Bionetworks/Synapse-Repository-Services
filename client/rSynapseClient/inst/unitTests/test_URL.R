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

unitTestGetPath <- function(){
	url <- URL(.getCache("exURL"))
	checkEquals(url@path, "data01.sagebase.org")
}

unitTestGetFileName <- function(){
	url <- URL(.getCache("exURL"))
	checkEquals(url@file, "mskcc_prostate_cancer.phenotype.zip")
}

unitTestGetQueryString <- function(){
	url <- URL(.getCache("exURL"))
	checkEquals(url@queryString, "Expires=1307658150&AWSAccessKeyId=AKIAI3BTGJG752CCJUVA&Signature=sN%2FNePyyQnkKwOWgTOxnLB5f42s%3D")
}

unitTestGetFullPath <- function(){
	url <- URL(.getCache("exURL"))
	checkEquals(url@fullFilePath, "data01.sagebase.org/mskcc_prostate_cancer.phenotype.zip")
}

unitTestGetPath <- function(){
	url <- URL(.getCache("exURL"))
	checkEquals(url@path, "data01.sagebase.org")
}


