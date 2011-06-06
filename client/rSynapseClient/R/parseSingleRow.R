parseSingleRow <- 
		function(row.list)
{
	#constants
	kNaValue <- NA
	kMultValDelimiterString <- ', '
	#end constants
	
	# clean field names to remove illegal characters, etc.
	names(row.list) <- cleanFieldNames(names(row.list))
	
	#iterate through rownames and add each element to the data frame
	row <- data.frame()
	for(field.name in names(row.list)){
		this.value <- row.list[[field.name]]
		if(is.null(this.value)){
			#substitue missing data for the NA string
			this.value <- kNaValue
		}else{
			# collapse multiple values down to a single delimited string
			this.value <- paste(row.list[[field.name]], collapse = kMultValDelimiterString)
		}
		row[1, field.name] <- this.value
	}
	return(row)
}
