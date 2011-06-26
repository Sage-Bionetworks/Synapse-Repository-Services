.jsonListToDataFrame <- function(jsonList){
	returnVal <- NULL
	for(i in 1:length(jsonList)){
		thisRow <- .parseSingleRow(jsonList[[i]])
		if(is.null(returnVal)){
			returnVal <- thisRow
		}else{
			returnVal <- .rowMerge(returnVal, thisRow)
		}		
	}
	return(returnVal)
}
