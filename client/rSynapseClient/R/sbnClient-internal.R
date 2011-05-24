setGenericVerif <- function(name,def){
	if(!isGeneric(name)){
		setGeneric(name,def)
	}else{
		#do nothing
	}
}

getClientConfig <- function(){
	if(!exists(getClientConfigName(), envir = getClientConfigEnvironment())){
		setClientConfig(new(Class="ClientConfig"))
	}
	config <- get(getClientConfigName(), envir = getClientConfigEnvironment())
	return(config)
}

getClientConfigName <- function(){
	kClientConfigName <- ".client.config"
	return(kClientConfigName)
}

getClientConfigEnvironment <- function(){
	envir <- globalenv()
	return(envir)
}

cleanFieldNames <- function(field.names){
	kRegularExpression <- "[[:punct:][:space:]]+"
	return(tolower(gsub(kRegularExpression, ".", field.names)))
}




