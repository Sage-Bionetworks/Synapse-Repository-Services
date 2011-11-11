setGeneric(
	name = "stopStep",
	def = function(step){
		standardGeneric("stopStep")
	}
	)

setMethod(
	f = "stopStep",
	signature = "SynapseEntity",
	definition = function(step){
		stopStep(propertyValue(step, "id"))
	}
	)

setMethod(
	f = "stopStep",
	signature = "numeric",
	definition = function(step) {
		stopStep(as.character(step))
	}
	)

setMethod(
	f = "stopStep",
	signature = "missing",
	definition = function(step) {
		stopStep(NA_character_)
	}
	)

setMethod(
	f = "stopStep",
	signature = "character",
	definition = function(step) {
		# If we were not passed a step, stop the current step
		if(missing(step) || is.na(step)) {
			step <-	.getCache("currentStep")
			if(is.null(step)) {
				stop("There is no step to stop")
			}
		}
		
		step <- getEntity(step)
		propertyValue(step, "endDate") <- .nowAsString()
		propertyValue(step, "environmentDescriptors")	<- .appendSessionInfoToDescriptors(propertyValue(step, "environmentDescriptors"))
		annotValue(step, "rHistory") <- .getRHistory()
		step <- updateEntity(step)
		.setCache("previousStep", step)
		.deleteCache("currentStep")
		step
	}
	)

.appendSessionInfoToDescriptors <- function(descriptors) {
	info <- sessionInfo()
	osDescriptor <- list(type="OS", name=info$R.version$platform)
	rDescriptor <- list(type="application", name="R", quantifier=info$R.version$version.string)
	listOfLists <- c(list(osDescriptor, 
												rDescriptor),
									 lapply(info$basePkgs, .makeRPackageDescriptor),
									 array(lapply(info$otherPkgs, .makeRPackageDescriptor)))	
	if(missing(descriptors) || is.null(descriptors)) {
		listOfLists
	} else {
		c(descriptors, listOfLists)
	}
}

.makeRPackageDescriptor <- function(packageDescription) {
	descriptor <- list()
	descriptor['type'] <- 'rPackage'
	if(is.character(packageDescription)) {
		descriptor['name'] <- packageDescription
	} else {
		descriptor['name'] <- packageDescription$Package
		descriptor['quantifier'] <- packageDescription$Version
	}
	descriptor
}

.getRHistory <- function(step) {
	file1 <- tempfile("Rrawhist")
	historyAttemptStatus <- try(savehistory(file1), silent=TRUE)
	if(is.null(historyAttemptStatus)) {
		rawhist <- readLines(file1)
		unlink(file1)
		rawhist
	}
	else {
		# there was no history
		return("")
	}
}