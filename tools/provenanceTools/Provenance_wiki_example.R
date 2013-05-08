library(synapseClient)

synapseLogin()

# Create project and data
projName <- sprintf("Provenances Project %s", as.character(gsub("-",".",Sys.Date())))
project <- createEntity(Project(list(name=projName)))
projectId <- propertyValue(project, "id")
myAnnotationFile <- createEntity(Data(list(name="myAnnotationFile", parentId=projectId)))
myRawDataFile <- createEntity(Data(list(name="myRawDataFile", parentId=projectId)))
myOutputFile <- createEntity(Data(list(name="myOutputFile", parentId=projectId)))
myScript <- createEntity(Data(list(name="myScript", parentId=projectId)))

########### EX 1
# Say we have some synapse entity already created with id syn789

# Create an activity describing what was done
activity<-Activity(list(name="Manual Curation"))

# Persisting the activity in synapse at this point is optional.
# If omitted, the activity will be persisted in the storeEntity
# command
activity<-createEntity(activity)

# Connect that activity to our file entity with a "generatedBy" relationship.
generatedBy(myOutputFile)<-activity
# Store generatedBy relationship in Synapse
myOutputFile<-storeEntity(myOutputFile)
activity<-generatedBy(myOutputFile) # need to update the 'activity' object

onWeb(myOutputFile)

########## EX 2
# Assuming we already have our output file (syn789) an annotation file (syn123) and a raw data file (syn456) in Synapse

# Create an activity describing the combination of myAnnotationFile and myRawDataFile to generate myOutputFile with the "used" list, 
# and connect that activity to our file entity with a "generatedBy" relationship.
activity<-Activity(list(name="Manual Annotation of Raw Data", used=list(list(entity=myAnnotationFile, wasExecuted=F), list(entity=myRawDataFile, wasExecuted=F))))
activity<-createEntity(activity)

# Connect that activity to our file entity with a "generatedBy" relationship.
generatedBy(myOutputFile)<-activity
# Store generatedBy relationship in Synapse
myOutputFile<-storeEntity(myOutputFile)
activity<-generatedBy(myOutputFile) # need to update the 'activity' object

onWeb(myOutputFile)


########## EX 3

# Using the "used" convenience method, create an activity describing the execution of Script.R in GitHub on the combination of 
# myAnnotationFile and raw data from GEO to generate myOutputFile with the "used" list
used(myOutputFile)<-list(list(name="Script.R", url="https://raw.github.com/username/RepoName/path/Script.R", wasExecuted=T), list(entity=myAnnotationFile, wasExecuted=F), list(name="GSM349870.CEL", url="http://www.ncbi.nlm.nih.gov/geo/download/?acc=GSM349870&format=file&file=GSM349870%2ECEL%2Egz", wasExecuted=F))

# Persist both new Activity and generatedBy relationship
myOutputFile<-storeEntity(myOutputFile)

onWeb(myOutputFile)


########## EX 4

# ======== Execute a script and store the result in a file: /tmp/file.txt ========

# Create an activity describing the execution of myScript with myAnnotationFile and myRawDataFile as inputs 
activity<-Activity(list(name="Scripted Annotation of Raw Data", used=list(list(entity=myScript, wasExecuted=T), list(entity=myAnnotationFile, wasExecuted=F), list(entity=myRawDataFile, wasExecuted=F))))
activity<-createEntity(activity)

# Load new file into myOutputFile and connect Activity all at once
myOutputFile <- addFile(myOutputFile, "/tmp/myOutputFile.txt")
generatedBy(myOutputFile)<-activity
myOutputFile <- storeEntity(myOutputFile)
activity<-generatedBy(myOutputFile) # need to update the activity object

onWeb(myOutputFile)

########## EX 5

# ======== Run script and store two files: /tmp/data.txt and /tmp/plot.png ========

# Create an activity describing the execution of myScript with myRawDataFile as the input
activity<-Activity(list(name="Process data and plot", used=list(list(entity=myScript, wasExecuted=T), list(entity=myRawDataFile, wasExecuted=F))))
activity<-createEntity(activity)

# Load two output entities myOutputFile and myPlot and connect Activity
myOutputFile <- addFile(myOutputFile, "/tmp/myOutputFile.txt")
generatedBy(myOutputFile)<-activity
myOutputFile <- storeEntity(myOutputFile)
activity<-generatedBy(myOutputFile) # need to update the activity object

# Create plot entity and store image in it
myPlot <- createEntity(Data(list(name="myPlot", parentId=propertyValue(project, "id"))))
myPlot <- addFile(myOutputFile, "/tmp/plot.png")
generatedBy(myPlot)<-activity
myPlot <- storeEntity(myPlot)

onWeb(myOutputFile)
