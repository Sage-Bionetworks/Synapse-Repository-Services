synapseDelete <- 
		function(uri, host = .getRepoEndpointLocation(), curlHandle=getCurlHandle(), anonymous=FALSE, 
				path = .getRepoEndpointPrefix(), opts = .getCache("curlOpts"))
{
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