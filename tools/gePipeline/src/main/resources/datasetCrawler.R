system("perl src/main/resources/getGSEs.pl")
all.gses <- read.table("all.GSEs.txt",sep="\t",header=TRUE,stringsAsFactors=FALSE,row.names=1)
all.gses <- all.gses[which(all.gses$Number_of_Samples < 1500),]
all.gses <- all.gses[order(all.gses$Number_of_Samples),]
"GSE.ID\tGPL\tLast Update Date\tSpecies\tDescription\tSupplementary_File\tNumber_of_Samples\tInvestigator\tPlatform\n";

output <- list()
lapply(1:nrow(all.gses), function(i) {  
			b <- list(gpl     =all.gses[i,1],
					lastUpdate    =all.gses[i,2],
					Species =all.gses[i,3],
					description =all.gses[i,4],
					hasCelFile=all.gses[i,5],
					number_of_samples=all.gses[i,6],
					creator=all.gses[i,7]
					#,Platform=all.gses[i,8]
			)
  b
}) -> output

names(output) <- rownames(all.gses)

source('./src/main/resources/synapseWorkflow.R')

finishWorkflowTask(output=output)
