library(synapseClient)
synapseLogin()

## this is just to show what's happening under the covers, you don't need to call this
onWeb(getStep())

## load some clinical data
clinicalData <- getEntity(2967)
# --> refresh the Step web UI page and see how it changed

## load some expression data
expressionData <- getEntity(2969)
# --> refresh the Step web UI page and see how it changed

####
# Do some science here
# . . .
# and get some interesting results worth saving in Synapse
####


## Create a project for results
myName <- "Nicole A. Deflaux"
project <- Project(list(
	name=paste("Machine Learning Results - ", myName)
	))
project <- createEntity(project)
## Create a dataset for results
dataset <- Dataset(list(
	name="Analysis Plots",
	parentId=propertyValue(project, "id")
	))
dataset <- createEntity(dataset)

## Create a Graph
attach(mtcars)
plot(wt, mpg) 
abline(lm(mpg~wt))
title("Regression of MPG on Weight")
outputFileElasticNet <- "/Users/deflaux/mygraph.jpg"
jpeg(outputFileElasticNet)

# Store the resulting graph in Synapse
elasticNetLayer <- Layer(list(
	name="ElasticNet Results for PLX4720",
	type="M", 
	parentId=propertyValue(dataset, "id")))
elasticNetLayer <- addFile(elasticNetLayer, outputFileElasticNet)
elasticNetLayer <- storeEntity(elasticNetLayer)
# --> refresh the Step web UI page and see how it changed

## I'm going to share what I did with my colleagues
analysis <- Analysis(list(description="glmnet algorithm applied to Cell Line Data and Sanger Drug Response data", 
													name="myFirstAnalysis",
													parentId=propertyValue(project, "id")))
analysis <- createEntity(analysis)
# --> refresh the Step web UI page and see how it changed

## q() will also do this
stoppedStep <- stopStep()
# --> refresh the Step web UI page and see how it changed
