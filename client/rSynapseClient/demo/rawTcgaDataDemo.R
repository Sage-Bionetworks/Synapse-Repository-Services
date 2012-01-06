library(synapseClient)

# There are many more datasets in there than just the TCGA ones, but we are only interested in TCGA data
datasets <- synapseQuery(paste('select * from dataset where dataset.Institution == "TCGA"', sep=""))
dim(datasets)
datasets[, 'dataset.name']

# We want to work with Glioblastoma data
glioblastomaDatasetId <- datasets$dataset.id[grepl('Glioblastoma TCGA', datasets$dataset.name )]
onWeb(glioblastomaDatasetId)

# Query for the the Level_3 layers for dataset "Glioblastoma TCGA"
layers <- synapseQuery(paste('select * from layer where layer.tcgaLevel == "Level_3" and layer.parentId == "', glioblastomaDatasetId, '"', sep=''))
dim(layers)
names(layers)
head(layers$layer.name)

agilentDataId <- layers$layer.id[grepl("unc.edu_GBM.AgilentG4502A_07_2.Level_3.4.0.0", layers$layer.name)]
onWeb(agilentDataId)

agilentData <- loadEntity(agilentDataId)
agilentData
agilentData$cacheDir
agilentData$files

# Get the clinical layers for dataset "Glioblastoma TCGA"
clinicalLayers <- synapseQuery(paste('select * from layer where layer.type == "C" and layer.parentId == "', glioblastomaDatasetId, '"', sep=''))
dim(clinicalLayers)
clinicalLayers$layer.name
clinicalData <- loadEntity(clinicalLayers[2, 'layer.id'])
clinicalData
clinicalData$cacheDir
clinicalData$files


