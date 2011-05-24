setClass(
		Class = "ClientConfig", 
		representation(
				host="character",
				sslhost="character",
				session.token="character"
		),
		prototype = prototype(
				host="http://repositoryservice.sagebase.org",
				sslhost="http://auth-sagebase-org.elasticbeanstalk.com",
				session.token=NULL
		)
)

setMethod(
		f = "show", 
		signature = "ClientConfig", 
		definition = function(object){
			cat("host = ", object@host, "\nsslhost = ", object@sslhost, "\nsession.token = ", object@session.token, "\n", sep="")
		}
)

setGenericVerif <- function(name,def){
	if(!isGeneric(name)){
		setGeneric(name,def)
	}else{
		#do nothing
	}
}

setGenericVerif(
		name = "isTokenValid",
		def = function(object){
			standardGeneric("isTokenValid")
		}
)

setMethod(
		f = "isTokenValid",
		signature = "ClientConfig",
		definition = function(object){
			is.valid <- TRUE
			#session.token is NULL until the user logs in
			if(is.null(object@session.token)){
				is.valid <- FALSE
			}
			return(is.valid)
		}
)

setGenericVerif(
		name = "setClientConfig",
		def = function(object){
			standardGeneric("setClientConfig")
		}
)

setMethod(
		f = "setClientConfig",
		signature = "ClientConfig",
		definition = function(object){
			assign(getClientConfigName(), object, envir = getClientConfigEnvironment())
		}
)

