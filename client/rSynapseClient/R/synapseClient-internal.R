.cleanFieldNames <- function(field.names){

	# DevNote: if we clean the field names like this, then we cannot subsequently use them in Update 
	#	operations.  Therefore I commented out this code.
	
#	kRegularExpression <- "[[:punct:][:space:]]+"
#	return(tolower(gsub(kRegularExpression, ".", field.names)))

	return(field.names)
}




