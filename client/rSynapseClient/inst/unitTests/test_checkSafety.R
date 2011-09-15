.setUp <- function() {
	# this test can only be run against staging
	synapseClient:::.setCache("orig.authservice.endpoint", synapseAuthServiceEndpoint())
	synapseClient:::.setCache("orig.reposervice.endpoint", synapseRepoServiceEndpoint())
	synapseAuthServiceEndpoint("https://prodauth.fake.com/auth/v1")
	synapseRepoServiceEndpoint("https://prodrepo.fake.com/repo/v1")
}

.tearDown <- function() {
	synapseAuthServiceEndpoint(synapseClient:::.getCache("orig.authservice.endpoint"))
	synapseRepoServiceEndpoint(synapseClient:::.getCache("orig.reposervice.endpoint"))
	synapseClient:::.deleteCache("orig.authservice.endpoint")
	synapseClient:::.deleteCache("orig.reposervice.endpoint")
}

unitTestCheckSafety <- function() {
	# Trying to "watch the watchers" here
	checkException(synapseClient:::.testSafety())
}