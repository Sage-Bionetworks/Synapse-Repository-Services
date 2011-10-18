# TODO: Add comment
# 
# Author: furia
###############################################################################
## load the synapse client and login
library(synapseClient)
library(affy)
synapseLogin()

## set up a project
myName <- <your name>
projName <- sprintf("%ss Curation Project", myName)

## create a project object using it's constructor. The
## list contains name-value pairs of properties that should
## be added to the project. See help documentation for details
## on the properties that can be set. For projects, only name is
## required
myProj <- Project(list(name=projName))

## show the project. note that entity id is missing
myProj

## create the project in Synapse using createEntity. make sure to 
## catch the return value
myProj <- createEntity(myProj)

## now the Synapse Entity Id is populated
myProj

## create a dataset 
myDataset <- createEntity(Dataset(list(name="my Data", parentId=propertyValue(myProj, "id"))))

## view the dataset on the web to add a description
onWeb(myDataset)

## refresh the local copy of myDataset
myDataset <- refreshEntity(myDataset)

## download a metageo expression layer
geoEntityId <- "17365"
expr <- loadEntity(geoEntityId)

##inspect the contents
expr

## write the pm values to a text file
write.table(pm(expr$objects$expression[["HG-U133A"]]), file="pm.txt", sep="\t", quote=F, row.names=F)

## create a new expression layer
myExpr <- createEntity(Layer(list(name="curated expression", type="E", parentId = propertyValue(myDataset, "id"), status="curated")))

## add an annotation specifying the data format
annotValue(myExpr, "format") <- "sageBioCurated"

## add the pm data file to the entity
myExpr <- addFile(myExpr, "pm.txt", path="GSE10024/expression/affymetrix")

## store the data
myExpr <- storeEntity(myExpr)

## store the code used to generate the curated data
## this is a bit artificial, but in this case we'll just store the entire Rhistory.
## in the provenance features of Synapse may provide an easier way to do this.
savehistory(file.path(tempdir(), "history.R"))
curationCode <- Code(list(name="My Curation Script", parentId = propertyValue(myDataset, "id")))
curationCode <- addFile(curationCode, file.path(tempdir(), "history.R"))
curationCode <- storeEntity(curationCode)

onWeb(myDataset)










