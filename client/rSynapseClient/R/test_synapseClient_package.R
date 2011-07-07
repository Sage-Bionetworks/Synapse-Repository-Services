.testSafety <- function() {
	lapply(c(synapseAuthServiceEndpoint(), synapseRepoServiceEndpoint()), 
			function(svc) {
				if(!(grepl("staging", svc) || grepl("localhost", svc))) {
					stop("tests may only run against staging or your localhost")
				}
			}
	)
}

.test <- function(dir=system.file("unitTests", package="synapseClient"), testFileRegexp = "^test_.*\\.R$") {
	.runTestSuite(dir=dir, testFileRegexp=testFileRegexp, testFuncRegexp="^unitTest.+", suiteName="unit tests") 
}

.integrationTest <- function(dir=system.file("integrationTests", package="synapseClient"), testFileRegexp="^test_.*\\.R$")
{
	.runTestSuite(dir=dir, testFileRegexp=testFileRegexp, testFuncRegexp="^integrationTest.+", suiteName="integration tests") 
}

.runTestSuite <- function(dir, testFileRegexp, testFuncRegexp, suiteName) {
	
	## Make sure its okay to run this test suite
	.testSafety()
	
	.failure_details <- function(result) {
		res <- result[[1L]]
		if (res$nFail > 0 || res$nErr > 0) {
			Filter(function(x) length(x) > 0,
					lapply(res$sourceFileResults,
							function(fileRes) {
								names(Filter(function(x) x$kind != "success",
												fileRes))
							}))
		} else list()
	}
	
	require("RUnit", quietly=TRUE) || stop("RUnit package not found")
	RUnit_opts <- getOption("RUnit", list())
	if(.getCache("debug")) {
		RUnit_opts$verbose <- 10L
		RUnit_opts$silent <- FALSE
	} else {
		RUnit_opts$verbose <- 0L
		RUnit_opts$silent <- TRUE
	}
	RUnit_opts$verbose_fail_msg <- TRUE
	options(RUnit = RUnit_opts)
	suite <- defineTestSuite(name=paste("synapseClient RUnit Test Suite", suiteName), 
			dirs=dir,
			testFileRegexp=testFileRegexp,
			testFuncRegexp=testFuncRegexp,
			rngKind="default",
			rngNormalKind="default")
	result <- runTestSuite(suite)
	cat("\n\n")
	printTextProtocol(result, showDetails=FALSE)
	if (length(details <- .failure_details(result)) >0) {
		cat("\nTest files with failing tests\n")
		for (i in seq_along(details)) {
			cat("\n  ", basename(names(details)[[i]]), "\n")
			for (j in seq_along(details[[i]])) {
				cat("    ", details[[i]][[j]], "\n")
			}
		}
		cat("\n\n")
		stop(paste(suiteName, " tests failed for package synapseClient"))
	}
	result
}

