.setUp <- function() {
	# this test can only be run against staging
	.setCache("orig.authservice.endpoint", synapseAuthServiceEndpoint())
	.setCache("orig.reposervice.endpoint", synapseRepoServiceEndpoint())
	synapseAuthServiceEndpoint("https://prodauth.fake.com/auth/v1")
	synapseRepoServiceEndpoint("https://prodrepo.fake.com/repo/v1")
}

.tearDown <- function() {
	synapseAuthServiceEndpoint(.getCache("orig.authservice.endpoint"))
	synapseRepoServiceEndpoint(.getCache("orig.reposervice.endpoint"))
	.deleteCache("orig.authservice.endpoint")
	.deleteCache("orig.reposervice.endpoint")
}

unitTestCheckSafety <- function() {
	# Trying to "watch the watchers" here
	checkException(.testSafety())
}