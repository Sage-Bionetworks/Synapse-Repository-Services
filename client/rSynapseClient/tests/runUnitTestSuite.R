# This test is only run during R CMD check
# It should invoke all unit tests but no integration tests
require("synapseClient") || stop("unable to load synapseClient package")
synapseClient:::.test()
