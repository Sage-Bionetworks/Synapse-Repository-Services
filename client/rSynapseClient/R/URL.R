# This is modeled after http://download.oracle.com/javase/6/docs/api/java/net/URL.html
# but with a few additional fields
# 
# Author: mfuria
###############################################################################

setClass(
		Class = ".ParsedUrl",
		representation = representation(
							url = "character",
							protocol = "character",
							authority = "character",
							host = "character",
							port = "character",
							queryString = "character",
							path = "character",
							file = "character",
							pathPrefix = "character"
						),
		prototype = prototype(url = NULL)
)

.ParsedUrl <- function(url){
	
	## protocol
	if(length(grep("://", url)) == 0){
		protocol <- ""
	}else{
		protocol <- gsub("://.+$","",url)
	}
	## host
	if(protocol == ""){
		tmp <- gsub("^/+", "", url)
	}else{
		tmp <- gsub("^.+://", "", url)
	}
	authority <- gsub("/.+$", "", tmp)
	if(grepl(":", authority)) {
		port <- gsub("^[^:]+:", "", authority)
		host <- gsub(":\\d+$", "", authority)
	}
	else {
		port <- ""
		host <- authority
	}
	rm(tmp)
	
	##query string
	queryString <- ""
	if(length(grep("\\?", url)) != 0){
		queryString <- gsub("^.+\\?", "", url)
	}
	
	## full path
	tmp <- gsub(paste("^", protocol, "://", authority, sep=""), "", url)
	path <- gsub(paste("\\?", queryString, "$", sep=""), "", tmp)
	
	## file
	splits <- strsplit(path, "/")
	file <- splits[[1]][length(splits[[1]])]
	
	## pathPrefix
	pathPrefix <- gsub(paste("/", file, "$", sep=""), "", path)
	
	new(
		Class = ".ParsedUrl",
		url = url,
		protocol = protocol,
    authority = authority,
		host = host,
    port = port,
		queryString = queryString,
		path = path,
		file = file,
		pathPrefix = pathPrefix
	)
}
