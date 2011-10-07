.testSafety <- function() {
	lapply(c(synapseAuthServiceEndpoint(), synapseRepoServiceEndpoint()), 
			function(svc) {
				if(!(grepl("staging", svc) || grepl(":8080", svc))) {
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
	if(synapseClient:::.getCache("debug")) {
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


testHMACSignature <- function() {
	# in this case the secret key has a null
	userId <- "matt.furia@sagebase.org" 
	uri <- "/services-repository-0.7-SNAPSHOT/repo/v1/project"
	timeStampString <- "2011-09-28T13:31:16.90-0700"
	base64EncodedSecretKey <- "GX/ZL7HPHOO4MvEUdADASuY8zmdKR10vINnNZ1lPLwkZZI/BYgl+FUyw35/NEhTFB1ZwGVQbVqVAA6w/0nbUYQ=="
	signature<-.generateHMACSignature(uri, timeStampString, userId, base64EncodedSecretKey)
	expected<-"rIi/ut4jdroxisbMsbvV0fuW9eQ="
	if (signature!=expected) stop("Error in testHMACSignature")
	
    # in this case the HMAC signature has a null
	userId <- "matt.furia@sagebase.org" 
	uri <- "/repo/v1/entity/17428/type"
	timeStampString <- "2011-10-07T00:09:40.44-0700"
	base64EncodedSecretKey <- "pDfk2KtmuvwFNKJzOn16ZfIY5qbSDebNFpTPHd6DuGemivMLWCV3tBFny6qGQ3luwXW7Q13IL3SUYC29mXeKdg=="
	signature<-.generateHMACSignature(uri, timeStampString, userId, base64EncodedSecretKey)
	expected<-"tVJCGnmI/8nh+W6CVgAJpv902Ns="
	if (signature!=expected) stop("Error in testHMACSignature")
}

