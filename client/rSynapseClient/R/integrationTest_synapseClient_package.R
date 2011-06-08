.integrationTest <- function(dir, pattern = "^test_.*\\.R$")
{
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

    if (missing(dir)) {
        dir <- system.file("integrationTests", package="synapseClient")
        if (!length(dir)) {
            dir <- system.file("IntegrationTests", package="synapseClient")
            if (!length(dir))
                stop("unable to find integration tests, no 'integrationTests' dir")
        }
    }
    require("RUnit", quietly=TRUE) || stop("RUnit package not found")
    RUnit_opts <- getOption("RUnit", list())
    RUnit_opts$verbose <- 0L
    RUnit_opts$silent <- TRUE
    RUnit_opts$verbose_fail_msg <- TRUE
    options(RUnit = RUnit_opts)
    suite <- defineTestSuite(name="synapseClient RUnit Tests", dirs=dir,
                             testFileRegexp=pattern,
                             testFuncRegexp = "^integrationTest.+",
                             rngKind="default",
                             rngNormalKind="default")
    result <- runTestSuite(suite)
    cat("\n\n")
    printTextProtocol(result, showDetails=FALSE)
    ## Print detailed html protocol, this is nice and readable
    printHTMLProtocol(result, "integrationTestOutput.html")
    if (length(details <- .failure_details(result)) >0) {
        cat("\nTest files with failing tests\n")
        for (i in seq_along(details)) {
            cat("\n  ", basename(names(details)[[i]]), "\n")
            for (j in seq_along(details[[i]])) {
                cat("    ", details[[i]][[j]], "\n")
            }
        }
        cat("\n\n")
        stop("integration tests failed for package synapseClient")
    }
    result
}
