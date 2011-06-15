# TODO: Add comment
# 
# Author: mfuria
###############################################################################

setClass(
		Class = "URL",
		representation = representation(
							url = "character",
							protocol = "character",
							host = "character",
							queryString = "character",
							fullFilePath = "character",
							file = "character",
							path = "character"
						),
		prototype = prototype(url = NULL)
)

URL <- function(url){
	
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
	host <- gsub("/.+$", "", tmp)
	rm(tmp)
	
	##query string
	queryString <- ""
	if(length(grep("\\?", url)) != 0){
		queryString <- gsub("^.+\\?", "", url)
	}
	
	## full path
	tmp <- gsub(paste("^", protocol, "://", host, "/", sep=""), "", url)
	fullFilePath <- gsub(paste("\\?", queryString, "$", sep=""), "", tmp)
	
	## file
	splits <- strsplit(fullFilePath, "/")
	file <- splits[[1]][length(splits[[1]])]
	
	## path
	path <- gsub(paste("/", file, "$", sep=""), "", fullFilePath)
	
	new(
		Class = "URL",
		url = url,
		protocol = protocol,
		host = host,
		queryString = queryString,
		fullFilePath = fullFilePath,
		file = file,
		path = path
	)
}
