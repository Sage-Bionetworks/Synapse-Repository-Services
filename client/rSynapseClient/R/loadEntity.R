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
			
			.loadCachedObjects(entity)
			
			if(is.null(annotValue(entity, "format"))){
				setPackageName(sprintf("entity%s", propertyValue(entity, "id")), env = entity@objects)
				return(entity)
			}
			entity@objects <- switch(annotValue(entity, "format"),
					rbin = .loadRbinaryFiles(file.path(entity@location@cacheDir,entity@location@files)),
					sageBioCurated = .loadSageBioPacket(entity),
					new.env()
			)
			setPackageName(sprintf("entity%s", propertyValue(entity, "id")), env = entity@objects)
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
			objects <- new.env()
			objects$phenotypes <- new("AnnotatedDataFrame", data = d, varMetadata = desc)
			objects
		}
)

setMethod(
		f = "loadEntity",
		signature = "ExpressionLayer",
		definition = function(entity){
			if(length(entity$files) == 0L)
				entity <- downloadEntity(entity)
			if(is.null(annotValue(entity, "format"))){
                                oldClass <- class(entity)
                                class(entity) <- "Layer"
                                entity <- loadEntity(entity)
                                class(entity) <- oldClass
			} else if(annotValue(entity,"format") == "GEO"){
				cel.files <- list.celfiles(entity$cacheDir, full.names=TRUE)
				cdfs <- sapply(cel.files, whatcdf) 
				expression <- lapply(unique(cdfs), function(cdf){
							ReadAffy(filenames=names(which(cdfs==cdf)))
						}
				)
				names(expression) <- unique(cdfs)
				entity <- addObject(entity, expression, unlist = FALSE)
			} else
				cat("No functionality to handle that kind of format yet - objects not loaded\n")
			entity
		}
)


setMethod(
		f = "loadEntity",
		signature = "Code",
		definition = function(entity){
			## call the super class load method
			oldClass <- class(entity)
			class(entity) <- "Layer"
			entity <- loadEntity(entity)
			class(entity) <- oldClass
			indx <- grep("\\.r$", tolower(entity$files))
			setPackageName(sprintf("entity%s", propertyValue(entity, "id")), env = entity@objects)
			tryCatch(
					lapply(entity$files[indx],
							function(f){
								f <- file.path(entity$cacheDir, f)
								sys.source(f, env = entity@objects)
							}
					),
					error = function(e){
						warning(e)
					}
			)
			entity
		}
)
