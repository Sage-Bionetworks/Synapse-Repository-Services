synapsePut <- 
		function(uri, entity, host = synapseRepoServiceHostName(), curlHandle = getCurlHandle(), anonymous = .getCache("anonymous"), path = .getCache("repoServicePath"), opts = .getCache("curlOpts"))
{
	## constants
	kMethod <- "PUT"
	## end constants

	.synapsePostPut(uri = uri, 
			entity = entity, 
			requestMethod = kMethod,
			host = host,
			curlHandle = curlHandle, 
			anonymous = anonymous, 
			path = path, 
			opts = opts
	)
}