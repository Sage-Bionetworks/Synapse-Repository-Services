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