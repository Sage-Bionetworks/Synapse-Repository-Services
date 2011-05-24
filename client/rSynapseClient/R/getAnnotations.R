getAnnotations <- function(id){
	kPath <- 'repo/v1/dataset'
	if(length(id) != 1){
		stop("multiple IDs provided")
	}
	uri <- paste(sbnHostName(), kPath, id, "annotations", sep="/")
	body <- getURL(uri)
	json.list <- fromJSON(body)
	return(json.list)
}

