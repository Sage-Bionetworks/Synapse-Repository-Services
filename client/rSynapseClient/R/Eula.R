# Sign End User License Agreement
# 
# Author: Matt Furia
###############################################################################

setMethod(
		f = "Eula",
		signature = "character",
		definition = function(entity){
			getEntity(entity)
		}
)

setMethod(
		f = "Eula",
		signature = "numeric",
		definition = function(entity){
			Eula(as.character(entity))
		}
)

setMethod(
		f = "Eula",
		signature = "list",
		definition = function(entity){
			eula <- SynapseEntity(entity=entity)
			
			## coerce to Eula
			class(eula) <- "Eula"
			synapseEntityKind(eula) <- synapseEntityKind(new(Class="Eula"))
			eula
		}
)

setMethod(
		f = ".signEula",
		signature = "Dataset",
		definition = function(entity){
			if(is.null(propertyValue(entity, "eulaId")))
				invisible(NULL)
			kService <- "/agreement"
			entity <- list(
					datasetId = propertyValue(entity, "id"),
					eulaId = propertyValue(entity, "eulaId")
			)
			synapsePost(kService, entity)
		}
)

setMethod(
		f = ".signEula",
		signature = "Layer",
		definition = function(entity){
			dataset <- getParentEntity(entity)
			.signEula(dataset)
		}
)

setMethod(
		f = ".signEula",
		signature = "character",
		definition = function(entity){ 
			.signEula(getEntity(entity))
		}
)

setMethod(
		f = ".signEula",
		signature = "numeric",
		definition = function(entity){
			.signEula(getEntity(entity))
		}
)

setMethod(
		f = ".signEula",
		signature = "list",
		definition = function(entity){
			.signEula(getEntity(entity))
		}
)

setMethod(
		f = ".signEula",
		signature = "SynapseEntity",
		definition = function(entity){
			stop("Only Layers and Datasets have EULAs")
		}
)

setMethod(
		f = "showEula",
		signature = "Eula",
		definition = function(entity){
			eulaFile <- tempfile()
			cat(propertyValue(entity, "agreement"), file = eulaFile)
			file.show(eulaFile, delete.file = TRUE)
		}
)

setMethod(
		f = "showEula",
		signature = "Dataset",
		definition = function(entity){
			eula <- getEntity(propertyValue(entity, "eulaId"))
			showEula(eula)
		}
)

setMethod(
		f = "showEula",
		signature = "Layer",
		definition = function(entity){
			dataset <- getParentEntity(entity)
			showEula(dataset)
		}
)

setMethod(
		f = ".promptEulaAgreement",
		signature = "Eula",
		definition = function(entity){
			kMaxPrompt <- 5L
			kAffirmativeResponses <- c("y", "yes")
			kNegativeResponses <- c("n", "no")
			
			showEula(entity)
			response <- "init"
			count <- 0
			
			while(!(tolower(response) %in% c(kAffirmativeResponses, kNegativeResponses))){
				if(count == kMaxPrompt)
					break
				response <- tolower(readline(prompt = "Do you agree to the EULA terms? [y/n]: "))
				count <- count + 1
			}
			
			if(response %in% kAffirmativeResponses){
				return(TRUE)
			} else{
				return(FALSE)
			}
		}
)

setMethod(
		f = ".promptEulaAgreement",
		signature = "Layer",
		definition = function(entity){
			dataset <- getParentEntity(entity)
			.promptEulaAgreement(dataset)
		}
)

setMethod(
		f = ".promptEulaAgreement",
		signature = "Dataset",
		definition = function(entity){
			eula <- getEntity(propertyValue(entity, "eulaId"))
			.promptEulaAgreement(eula)
		}
)


.promptSignEula <- function(){
	kMaxPrompt <- 5L
	kAffirmativeResponses <- c("y", "yes")
	kNegativeResponses <- c("n", "no")
	response <- "init"
	count <- 0L
	cat("You must sign the End-user License Agreement before downloading this entity.\n")
	while(!(tolower(response) %in% c(kAffirmativeResponses, kNegativeResponses))){
		if(count == kMaxPrompt)
			break
		response <- tolower(readline(prompt="Would you like to sign it now? [y/n]: "))
		count <- count + 1
	}
	if(response %in% kAffirmativeResponses){
		return(TRUE)
	} else{
		return(FALSE)
	}
}

setMethod(
		f = "hasSignedEula",
		signature = "Dataset",
		definition = function(entity){
			if(is.null(propertyValue(entity, "eulaId")))
				return(TRUE)
			queryString <- sprintf('select * from agreement where datasetId=="%s" and eulaId=="%s" and createdBy=="%s"', propertyValue(entity, "id"), propertyValue(entity, "eulaId"), as.character(.getCache("username")))
			agreements <- synapseQuery(queryString)
			if(is.null(agreements))
				return(FALSE)
			nrow(agreements) > 0
		}
)

setMethod(
		f = "hasSignedEula",
		signature = "Layer",
		definition = function(entity){
			hasSignedEula(getParentEntity(entity))
		}
)
