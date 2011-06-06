synapsePut <- 
		function(uri, httpBody, curlHandle = getCurlHandle(), anonymous = .getCache("anonymous"), path = .getCache("repoServicePath"), opts = .getCache("curlOpts"))
{
	## constants
	kMethod <- "PUT"
	## end constants

	synapsePostPut(uri = uri, 
			httpBody = httpBody, 
			requestMethod = kMethod, 
			curlHandle = curlHandle, 
			anonymous = anonymous, 
			path = path, 
			opts = opts
	)
}