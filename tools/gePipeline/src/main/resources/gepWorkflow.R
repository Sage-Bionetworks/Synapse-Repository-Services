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

summary <-inputDataMap[["Description"]]
gpl<-inputDataMap[["gpl"]]
hasCelFiles<-inputDataMap[["hasCelFiles"]]
species<-inputDataMap[["Species"]]
lastUpdate<-inputDataMap[["lastUpdate"]]
nSamples<-inputDataMap[["Number_of_Samples"]]

cat("SUMMARY: ");cat(summary);cat("\n\n")
cat("GPL: ");cat(gpl);cat("\n\n")
cat("hasCelFiles: ");cat(hasCelFiles);cat("\n\n")
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

