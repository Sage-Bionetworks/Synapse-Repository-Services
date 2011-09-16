# Load Entity Objects
# 
# Author: Matt Furia
###############################################################################

setMethod(
		f = "loadEntity",
		signature = "Layer",
		definition = function(entity){
			if(length(entity@location@files) == 0)
				entity <- downloadEntity(entity)
			
			if(is.null(annotValue(entity, "format")))
				return(entity)
			entity@loadedObjects <- switch(annotValue(entity, "format"),
					rbin = .loadRbinaryFiles(file.path(entity@location@cacheDir,entity@location@files)),
					sageBioCurated = .loadSageBioPacket(entity),
					new.env()
			)
			entity
		}
)

setMethod(
		f = "loadEntity",
		signature = "character",
		definition = function(entity){
			entity <- getEntity(entity)
			loadEntity(entity)
		}
)

setMethod(
		f = "loadEntity",
		signature = "numeric",
		definition = function(entity){
			loadEntity(as.character(entity))
		}
)

setMethod(
		f = ".loadSageBioPacket",
		signature = "SynapseEntity",
		definition = function(entity){
			stop("Only Layer entities can be loaded from SageBioCurated format")
		}
)

setMethod(
		f = ".loadSageBioPacket",
		signature = "Layer",
		definition = function(entity){
			
		}
)

setMethod(
		f = ".loadSageBioPacket",
		signature = "PhenotypeLayer",
		definition = function(entity){
			if(is.null(annotValue(entity, "format")) || annotValue(entity, "format") != "sageBioCurated")
				stop("Unable to load Phenotype layers that are not in SageBioCurated format using this function.")
			
			if(length(entity@location@files) == 0)
				entity <- downloadEntity(entity)
			
			files <- file.path(entity@location@cacheDir, entity@location@files)
			
			## look in phenotype directory for 
			indx <- grep("/phenotype/phenotype.txt$",files)
			d <- read.delim(files[indx], as.is=T)
			
			indx <- grep("/phenotype/description.txt$", files)
			desc <- read.delim(files[indx], as.is=T)
			
			indx <- grep("individuals.txt", files)
			indiv <- read.delim(files[indx], as.is=T)
			
			## TODO fix this hack after demo
			names(d)[1] <- "individual_id"
			names(indiv)[1] <- "individual_id"
			
			## return the data as an annotated data frame
			rownames(d) <- d[,1]
			d <- d[,-1]
			rownames(desc) <- desc[,1]
			desc <- desc[,-1]
			names(desc)[names(desc) == "description"] <- "labelDescription"
			loadedObjects <- new.env()
			loadedObjects$phenotypes <- new("AnnotatedDataFrame", data = d, varMetadata = desc)
			loadedObjects
		}
)

