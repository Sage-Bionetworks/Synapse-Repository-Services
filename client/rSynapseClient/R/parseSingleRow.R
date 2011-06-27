.parseSingleRow <- 
		function(row)
{
	## constants
	kNaValue <- NA
	kMultiValueDelimiterString <- ', '
	## end constants
	
	## clean field names to remove illegal characters, etc.
	names(row) <- .cleanFieldNames(names(row))
	
	## iterate through rownames and add each element to the data frame
	isNull <- sapply(row,is.null) 
	row[isNull] <- kNaValue 
	row[!isNull] <- lapply(row[!isNull], paste, collapse=kMultiValueDelimiterString)
	data.frame(row, stringsAsFactors=FALSE)
}
