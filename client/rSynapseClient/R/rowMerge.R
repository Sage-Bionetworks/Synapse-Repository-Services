.rowMerge <- function(table1, newRow){
	rowNumber <- dim(table1)[1] + 1
	for(colName in names(newRow)){
		table1[rowNumber, colName] <- newRow[1,colName]
	}
	return(table1)
}