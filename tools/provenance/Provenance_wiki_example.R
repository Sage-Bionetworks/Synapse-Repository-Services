library(synapseClient)

synapseLogin()

# Create project
projName <- sprintf("Provenances Project %s", as.character(gsub("-",".",Sys.Date())))
project <- createEntity(Project(list(name=projName)))
projectId <- propertyValue(project, "id")

# create some resources to use
myAnnotationFile <- synStore(File("~/tmp/myAnnotationFile.txt", parentId=projectId))
myRawDataFile <- synStore(File("~/tmp/myRawDataFile.txt", parentId=projectId))
myScript <- synStore(File("~/tmp/myScript.txt", parentId=projectId))

## Example 1
# Here we explicitly define an "Activity" object, then we create the myOutputFile 
# entity and use the activity to describe how it was made. 

activity<-createEntity(Activity(list(name="Manual Curation")))
myOutputFile <- synStore(File("~/tmp/myOutputFile.txt", parentId=projectId), activity)

onWeb(myOutputFile)

## Example 2
# Create an activity implicitly by describing it in the synStore call for myOutputFile. 
# Here the combination of myAnnotationFile and myRawDataFile are used to generate myOutputFile.

myOutputFile <- synStore(File("~/tmp/myOutputFile.txt", parentId=projectId), 
                         used=list(myAnnotationFile, myRawDataFile), 
                         activityName="Manual Annotation of Raw Data", 
                         activityDescription="...")

onWeb(myOutputFile)

## Example 3
# Create a provenance record for myOutputFile that uses resources external to Synapse. 
# Here we are describing the execution of Script.py (stored in GitHub) 
# where myAnnotationFile and a raw data file from GEO are used as inputs 
# to generate myOutputFile with the "used" list

myOutputFile <- synStore(File("~/tmp/myOutputFile.txt", parentId=projectId), 
                         used=list(list(name="Script.py", url="https://raw.github.com/.../Script.py", wasExecuted=T), 
                                   list(entity=myAnnotationFile, wasExecuted=F),
                                   list(name="GSM349870.CEL", url="http://www.ncbi.nlm.nih.gov/geo/download/...", wasExecuted=F)),
                         activityName="Scripted Annotation of Raw Data", 
                         activityDescription="To execute run: python Script.py [Annotation] [CEL]")

onWeb(myOutputFile)


## Example 4
# Create an activity describing the execution of myScript with myRawDataFile as the input

activity<-createEntity(Activity(list(name="Process data and plot", 
                                     used=list(list(entity=myScript, wasExecuted=T), 
                                               list(entity=myRawDataFile, wasExecuted=F)))))

# Record that the script's execution generated two output files, and upload those files

myOutputFile <- synStore(File("~/tmp/myOutputFile.txt", parentId=projectId), activity)
myPlot <- synStore(File("~/tmp/myPlot.png", parentId=projectId), activity)

onWeb(myOutputFile)

