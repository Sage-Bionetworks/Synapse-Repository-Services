# TODO: Add comment
# 
# Author: mfuria
###############################################################################

refreshSessionToken <- 
		function(session.token, host = synapseAuthServiceHostName())
{
	# constants
	kService <- "session"
	## end constants
	
	httpBody <- paste('{\"sessionToken\":\"',
			session.token, '\"}',
			sep=""
			)

	uri <- kService
	response <- synapsePut(uri = uri, httpBody = httpBody, path = .getCache("authServicePath"), host = host)
	.setCache("sessionTimestamp", Sys.time())
}
