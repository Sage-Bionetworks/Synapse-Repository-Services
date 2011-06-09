synapsePost <- 
		function(uri, entity, host = synapseRepoServiceHostName(), curlHandle = getCurlHandle(), anonymous = FALSE, path = .getCache("repoServicePath"), opts = .getCache("curlOpts"))
{
	## constants
	kMethod <- "POST"
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