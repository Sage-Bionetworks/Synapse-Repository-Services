setMethod(
		f = "loadLayerData",
		signature = "list",
		definition = function(entity)
		{
			## TODO: the difinition for this signature needs work
			.cacheFiles(entity)
		}
)

setMethod(
		f = "loadLayerData",
		signature = "character",
		definition = function(entity)
		{
			## get entity as S4 and dispatch to type-specific loader
			loadLayerData(getEntity(entity))
		}
)

setMethod(
		f = "loadLayerData",
		signature = "integer",
		definition = function(entity)
		{
			loadLayerData(as.character(entity))
		}
)

setMethod(
		f = "loadLayerData",
		signature = "PhenotypeLayer",
		definition = function(entity){
			files <- .cacheFiles(propertyValue(entity, "id"))
			if(!is.null(annotValue(entity, "format")) && annotValue(entity, "format") == "sageBioCurated"){
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
				
				return(new("AnnotatedDataFrame", data = d, varMetadata = desc))
			}	
			if(!is.null(annotValue(entity, "format")) && annotValue(entity, "format") == "rbin"){
				return(.loadRbinaryFiles(files))
			}
			files
		}
)

setMethod(
		f = "loadLayerData",
		signature = "Layer",
		definition = function(entity){
			## for a generic Layer, do format specific dispatch
			files <- .cacheFiles(propertyValue(entity, "id"))
			
			## check for known format specifications
			if(is.null(annotValue(entity, "format")))
				return(files)
			switch(annotValue(entity, "format"),
					rbin = .loadRbinaryFiles(files),
					files
			)
		}
)

.loadRbinaryFiles <- function(files){
	layerData <- new.env()
	for(f in files){
		tryCatch(
				load(f, envir = layerData),
				error=function(e){
					warning(sprintf("Could not load layer file %s\n", f))
			}
		)
	}
	return(layerData)
}

.loadTsvFiles <- function(files){
	returnVal <- list()
	for(f in files){
		tryCatch(
			returnVal[[f]] <- read.delim(f, as.is=T),
			error = function(e){
				warning(sprintf("Could not load layer file %s\n", f))
			}
		)
	}
	returnVal
}

.loadCsvFiles <- function(files){
	returnVal <- list()
	for(f in files){
		tryCatch(
				returnVal[[f]] <- read.delim(f, as.is=T, sep=","),
				error = function(e){
					warning(sprintf("Could not load layer file %s\n", f))
				}
		)
	}
	returnVal
}