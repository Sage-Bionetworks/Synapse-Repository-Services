source('./src/main/resources/synapseWorkflow.R')
source('./src/main/resources/datasetQueries.R')
source('./src/main/resources/synapseWorkflow.R')

system("perl src/main/resources/getGSEs.pl")
all.gses <- read.table("all.GSEs.txt",sep="\t",header=TRUE,stringsAsFactors=FALSE,row.names=1)
#all.gses <- all.gses[which(all.gses$Number_of_Samples < 1500),]
all.gses <- all.gses[order(all.gses$Number_of_Samples),]

# first, make all priority 1
all.gses<-cbind(all.gses, rep(1, dim(all.gses)[1]))
cat(paste("Crawler found", dim(all.gses)[1], "datasets to process...\n"))
PRIORITY_COLUMN_NAME <- "Priority"
colnames(all.gses)[dim(all.gses)[2]]<-PRIORITY_COLUMN_NAME

# now make the existing dataset in Synapse priority 3
userName <- getUsernameArg()
secretKey <- getSecretKeyArg()
authEndpoint <- getAuthEndpointArg()
repoEndpoint <- getRepoEndpointArg()
parentId <-getProjectId()
require("synapseClient")
synapseAuthServiceEndpoint(authEndpoint)
synapseRepoServiceEndpoint(repoEndpoint)
synapseClient:::userName(userName)
hmacSecretKey(secretKey)

existingDatasets <- getAllDatasets(parentId, verbose=F)
existingIndices <- unlist(intersect(rownames(all.gses), existingDatasets))
cat(paste("... of which", length(existingIndices), " are already in Synapse...\n"))
all.gses[existingIndices, PRIORITY_COLUMN_NAME]<- 3

# finally, make the failed dataset in Synapse priority 2
failedDatasets <- getFailedDatasets(parentId, verbose=F)
failedIndices <- unlist(intersect(rownames(all.gses), failedDatasets))
cat(paste("... and of which ", length(failedIndices), " are already in Synapse but encountered an error.\n"))
all.gses[failedIndices, PRIORITY_COLUMN_NAME]<- 2

# this leaves as Priority 1 those datasets which have not been 
# created in Synapse

# now sort by Priority
all.gses <-all.gses[order(all.gses$Priority),]

header <- "GSE.ID\tGPL\tLast Update Date\tSpecies\tDescription\tSupplementary_File\tNumber_of_Samples\tInvestigator\tPlatform\n";

output <- list()
lapply(1:nrow(all.gses), function(i) {  
      b <- list(gpl     =all.gses[i,1],
          lastUpdate    =all.gses[i,2],
          Species =all.gses[i,3],
          description =all.gses[i,4],
          hasCelFiles=all.gses[i,5],
          Number_of_Samples=all.gses[i,6],
		  createdBy=all.gses[i,7],
          Platform=all.gses[i,8],
          url=paste("ftp://ftp.ncbi.nih.gov/pub/geo/DATA/supplementary/series/",rownames(all.gses)[i],"/",rownames(all.gses)[i],"_RAW.tar",sep=""))
    }) -> output

names(output) <- rownames(all.gses)


finishWorkflowTask(output=output)