# TODO: Add comment
# 
# Author: mfuria
###############################################################################
.setUp <- function(){
	.setCache("testProjectName", paste('R Entity CRUD Integration Test Project', gsub(':', '_', date())))
}

.tearDown <- function(){
	if(!is.null(.getCache("testProject")))
		deleteEntity(.getCache("testProject"))	
}

integrationTestCreateS4Entities <- function(){
	## Create Project
	project <- new(Class="Project")
	project@properties$name <- .getCache("testProjectName")
	project <- createEntity(project)
	checkEquals(project@properties$name, .getCache("testProjectName"))
	
	## Create DataSet
	
	## Create Layer
	
	## Create Location
	
}

integrationTestS4Crud <- function(){
	## Create a project
	project <- Project(list(name = paste('R Entity CRUD Integration Test Project', gsub(':', '_', date()))))
	project <- createEntity(project)
	.setCache("testProject", project)
	
	## get the project annotations
	annotations(project) <- SynapseAnnotation(getAnnotations(entity = .extractEntityFromSlots(project)))
	## Add some annotations and update
	annotValue(project, "charValue") <- "thisIsACharacterAnnotation"
	project <- updateAnnotations(project)
	checkEquals(annotValue(project, "charValue"), "thisIsACharacterAnnotation")
	
	## Add another annotation and update only the annotations object
	annotValue(project, "longValue") <- 1L
	
	tmpAnnotations <- updateAnnotations(annotations(project))
	project <- refreshEntity(project)
	annotations(project) <- tmpAnnotations
	## create a dataset
	
	## modify the dataset
	
	## refresh the local object
	
	## add a layer
	
	
}


