getDataPacketSummary <- function(id){
	kPath <- "repo/v1/dataset"
	if(length(id) != 1){
		stop("multiple IDs provided")
	}
	uri <- paste(sbnHostName(), kPath, id,sep="/")
	body <- getURL(uri)
	results.list <- fromJSON(body)
	return.val <- parseSingleRow(results.list)
	
	return(return.val)
}

