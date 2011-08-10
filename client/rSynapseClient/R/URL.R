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
	## swap backslashes for forward slashes
	url <- gsub("[\\]", "/", url)
	
	## protocol
	if(length(grep("://", url)) == 0){
		protocol <- ""
	}else{
		protocol <- gsub("://.+$","",url)
	}
	
	## authority which is host[:port]
	if(protocol == ""){
		tmp <- gsub("^/+", "", url)
	}else{
		tmp <- gsub("^.+://", "", url)
	}
	
	## check for forward slashes before setting authority
	if(grepl("/",tmp)){
		authority <- gsub("/.+$", "", tmp)
	}else{
		authority <- ""
	}

	if(grepl(":", authority)) {
		port <- gsub("^[^:]+:", "", authority)
		host <- gsub(":\\d+$", "", authority)
	}
	else {
		port <- ""
		host <- authority
	}
	rm(tmp)
	
	## query string
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
	if(grepl("/", path)){
		pathPrefix <- gsub(paste("/", file, "$", sep=""), "", path)
	}else{
		pathPrefix <- ""
	}
		
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

setMethod(
	f = "show",
	signature = ".ParsedUrl",
	definition = function(object){
		for(sn in slotNames(object))
			cat(sn, "=", slot(object, sn), "\n", sep="")
	}
)