.setUp <- function(){
	.setCache("testInput","https://fakehost.com/fakePathPrefix.org/fakeFile.zip?Expires=1307658150&AWSAccessKeyId=AKIAI3BTGJG752CCJUVA&Signature=sN%2FNePyyQnkKwOWgTOxnLB5f42s%3D")
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
	checkEquals(url@host, "fakehost.com")
}

unitTestGetPathPrefix <- function(){
	url <- .ParsedUrl(.getCache("testInput"))
	checkEquals(url@pathPrefix, "/fakePathPrefix.org")
}

unitTestGetFileName <- function(){
	url <- .ParsedUrl(.getCache("testInput"))
	checkEquals(url@file, "fakeFile.zip")
}

unitTestGetQueryString <- function(){
	url <- .ParsedUrl(.getCache("testInput"))
	checkEquals(url@queryString, "Expires=1307658150&AWSAccessKeyId=AKIAI3BTGJG752CCJUVA&Signature=sN%2FNePyyQnkKwOWgTOxnLB5f42s%3D")
}

unitTestGetPath <- function(){
	url <- .ParsedUrl(.getCache("testInput"))
	checkEquals(url@path, "/fakePathPrefix.org/fakeFile.zip")
}

unitTestGetHostWithPort <- function() {
	url <- .ParsedUrl('http://fakeHost:0000/services-authentication-fakeRelease-SNAPSHOT/auth/v1')
	checkEquals(url@authority, 'fakeHost:0000')
	checkEquals(url@host, 'fakeHost')
	checkEquals(url@port, '0000')
	checkEquals(url@path, '/services-authentication-fakeRelease-SNAPSHOT/auth/v1')
	checkEquals(url@file, 'v1')
	checkEquals(url@pathPrefix, '/services-authentication-fakeRelease-SNAPSHOT/auth')
}
