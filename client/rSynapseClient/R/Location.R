
setMethod(
		f = "Location",
		signature = "character",
		definition = function(entity){
			Location(.getEntity(entity = entity))
		}
)

setMethod(
		f = "Location",
		signature = "list",
		definition = function(entity){
			## superclass constructor
			s4Entity <- SynapseEntity(entity=entity)
			## coerce the type to Location
			class(s4Entity) <- "Location"
			synapseEntityKind(s4Entity) <- synapseEntityKind(new(Class="Location"))
			return(s4Entity)
		}
)

setMethod(
		f = "Location",
		signature = "numeric",
		definition = function(entity){
			Location(as.character(entity))
		}
)

setMethod(
		f = "CachedLocation",
		signature = signature("Location", "character"),
		definition = function(location, files){
			class(location) <- "CachedLocation"
			location@cacheDir <- attr(files, "rootDir")
			files <- gsub(attr(files,"rootDir"), "", as.character(files))
			location@files <- gsub(sprintf("^%s", .Platform$file.sep), "", files)
			location
		}
)

setMethod(
		f = "show",
		signature = "CachedLocation",
		definition = function(object){
			cat('An object of class "', class(object), '"\n', sep="")
			
			cat("Synapse Entity Name : ", properties(object)$name, "\n", sep="")
			cat("Synapse Entity Id   : ", properties(object)$id, "\n", sep="")
			
			if (!is.null(properties(object)$parentId))
				cat("Parent Id           : ", properties(object)$parentId, "\n", sep="")
			if (!is.null(properties(object)$type))
				cat("Type                : ", properties(object)$type, "\n", sep="")
			if (!is.null(properties(object)$version))
				cat("Version             : ", properties(object)$version, "\n", sep="")
			
			msg <- summarizeCacheFiles(object)
			if(!is.null(msg)){
				cat("\n", msg$count, " :\n", sep="")
				cat(msg$files, sep="\n")
			}
			cat("\nFor complete list of properties, please use the properties() function.\n")
		}
)

setMethod(
		f = "summarizeCacheFiles",
		signature = "CachedLocation",
		definition = function(entity){
			## if Cached Files exist, print them out
			msg <- NULL
			if(length(entity@cacheDir) != 0){
				msg$count <- sprintf('%d File(s) cached in "%s"', length(entity@files), entity@cacheDir)
				if(length(entity@files) > 0)
					msg$files <- sprintf('[%d] "%s"',1:length(entity@files), entity@files)
			}
			msg
		}
)