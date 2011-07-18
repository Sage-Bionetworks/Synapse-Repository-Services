synapseDelete <- 
		function(uri, entity, host = .getRepoEndpointLocation(), curlHandle=getCurlHandle(), anonymous=FALSE, 
				path = .getRepoEndpointPrefix(), opts = .getCache("curlOpts"))
{
	## constants
	kMethod <- "DELETE"
	## end constants
	
	if(!missing(entity)){
		.synapsePostPut(uri = uri, 
				requestMethod = kMethod,
				entity = entity,
				host = host, 
				curlHandle = curlHandle, 
				anonymous = anonymous, 
				path = path, 
				opts = opts		
		)
	}else{
		.synapseGetDelete(uri = uri, 
				requestMethod = kMethod,
				host = host, 
				curlHandle = curlHandle, 
				anonymous = anonymous, 
				path = path, 
				opts = opts
		)
	}
}