synapseDelete <- function(uri, host = synapseRepoServiceHostName(), curlHandle=getCurlHandle(), anonymous = .getCache("anonymous"), path = .getCache("repoServicePath"), opts = .getCache("curlOpts")){
	## constants
	kMethod <- "DELETE"
	## end constants
	
	.synapseGetDelete(uri = uri, 
			requestMethod = kMethod, 
			host = host, 
			curlHandle = curlHandle, 
			anonymous = anonymous, 
			path = path, 
			opts = opts
	)
}