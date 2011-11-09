setGeneric(
	name = "startStep",
	def = function(parentEntity){
		standardGeneric("startStep")
	}
	)

setMethod(
	f = "startStep",
	signature = "SynapseEntity",
	definition = function(parentEntity){
		startStep(propertyValue(parentEntity, "id"))
	}
	)

setMethod(
	f = "startStep",
	signature = "numeric",
	definition = function(parentEntity) {
		startStep(as.character(parentEntity))
	}
	)

setMethod(
	f = "startStep",
	signature = "missing",
	definition = function(parentEntity) {
		startStep(NA_character_)
	}
	)

setMethod(
	f = "startStep",
	signature = "character",
	definition = function(parentEntity) {
		# Stop the current step, if applicable
		step <- .getCache("currentStep")
		if(!is.null(step)) {
			stopStep(step)
		}
		
		# Create a new step
		step <- Step(list(commandLine=paste(commandArgs(), collapse=" ")))
		if(!missing(parentEntity)) {
			propertyValue(step, "parentId") <- parentEntity
		}
		step <- createEntity(step)
		.setCache("currentStep", step)
		step
	}
	)

