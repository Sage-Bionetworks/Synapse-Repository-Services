synapseResetEndpoints <-
		function()
{
	synapseAuthServiceEndpoint("https://auth-alpha.sagebase.org/auth/v1")
	synapseRepoServiceEndpoint("https://reposvc-alpha.sagebase.org/repo/v1")
}