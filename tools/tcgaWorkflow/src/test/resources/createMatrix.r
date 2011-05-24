#!/usr/bin/env Rscript

# pass args like --args --layerId 42 --localFilepath ./foo.txt

args <- commandArgs(trailingOnly = TRUE)
print(args)