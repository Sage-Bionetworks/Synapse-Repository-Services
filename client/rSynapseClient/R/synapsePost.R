synapsePost <- 
		function(uri, httpBody, host = synapseRepoServiceHostName(), curlHandle = getCurlHandle(), anonymous = FALSE, path = .getCache("repoServicePath"), opts = .getCache("curlOpts"))
{
	## constants
	kMethod <- "POST"
	## end constants
	
	synapsePostPut(uri = uri, 
			httpBody = httpBody, 
			requestMethod = kMethod, 
			host = host, 
			curlHandle = curlHandle, 
			anonymous = anonymous, 
			path = path, 
			opts = opts
	)
}