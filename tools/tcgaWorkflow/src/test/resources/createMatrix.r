#!/usr/bin/env Rscript

library(sbnClient)

inputLayer <- getInputLayerId()

data <- read.table(getLocalFilepath(), sep='\t')

setOutputLayerId(inputLayer)
