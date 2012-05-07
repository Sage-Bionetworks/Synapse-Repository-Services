#!/usr/bin/env Rscript

print("Hello World! This should succeed.")
message("and here's some stderr")

library(synapseClient)
setApiCredentials(username=Sys.getenv("SYNAPSE_USERNAME"), secretKey=Sys.getenv("SYNAPSE_SECRET_KEY"))