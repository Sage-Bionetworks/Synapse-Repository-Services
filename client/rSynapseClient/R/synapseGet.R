synapseGet <- 
		function(uri, host = .getRepoEndpointLocation(), curlHandle=getCurlHandle(), anonymous = .getCache("anonymous"), 
				path = .getRepoEndpointPrefix(), opts = .getCache("curlOpts"), entity = NULL)
{
	## constants
	kMethod <- "GET"
	## end constants
	
	.synapseGetDelete(uri = uri, 
			requestMethod = kMethod, 
			host = host, 
			curlHandle = curlHandle, 
			anonymous = anonymous, 
			path = path, 
			opts = opts,
			entity = entity
	)
}

