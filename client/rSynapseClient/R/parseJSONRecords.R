.parseJSONRecords <- 
		function(json.list)
{
	results.table <- data.frame()
    if(0 < length(json.list)) {
		for(i in 1:length(json.list)){
			this.row <- .parseSingleRow(json.list[[i]])
			if(is.null(results.table)){
				results.table <- this.row
			}else{
				results.table <- .rowMerge(results.table, this.row)
            }
		}
	}
	return(results.table)
}
