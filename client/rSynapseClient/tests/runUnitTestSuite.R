# This test is only run during R CMD check
# It should invoke all unit tests but no integration tests
require("sbnClient") || stop("unable to load sbnClient package")
sbnClient:::.test()
