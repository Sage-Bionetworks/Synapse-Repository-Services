#!/usr/bin/env Rscript

source('./src/main/resources/synapseWorkflow.R')
library("RJSONIO")

gseId <- getInputDatasetIdArg()
userName <- getUsernameArg()
secretKey <- getSecretKeyArg()
authEndpoint <- getAuthEndpointArg()
repoEndpoint <- getRepoEndpointArg()
projectId <- getProjectIdArg()

urlEncodedInputData <- getInputDataArg()

inputData <- URLdecode(urlEncodedInputData)
cat(inputData)
cat("\n\n")

inputDataMap<-RJSONIO::fromJSON(inputData)

summary <-inputDataMap[["summary"]]
gpl<-inputDataMap[["gpl"]]
hasCelFile<-inputDataMap[["hasCelFile"]]
species<-inputDataMap[["species"]]
lastUpdate<-inputDataMap[["lastUpdate"]]
nSamples<-inputDataMap[["n_sample"]]

cat("SUMMARY: ");cat(summary);cat("\n\n")
cat("GPL: ");cat(gpl);cat("\n\n")
cat("hasCelFile: ");cat(hasCelFile);cat("\n\n")
cat("SPECIES: ");cat(species);cat("\n\n")
cat("lastUpdate: ");cat(lastUpdate);cat("\n\n")
cat("nSamples: ");cat(nSamples);cat("\n\n")


finishWorkflowTask(output=paste("datasetid:",gseId, 
				"Input Data:", inputData, 
				"userName:", userName,
				"secretKey:", secretKey,
				"authEndpoint:", authEndpoint,
				"repoEndpoint:", repoEndpoint
				))

