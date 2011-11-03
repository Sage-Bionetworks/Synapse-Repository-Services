setMethod(
		f = "edit",
		signature = signature("Code"),
		definition = function(name, which, ...){
			args <- as.list(substitute(list(...)))[-1L]
			if("file" %in% names(args))
				stop("file argument is currently not supported")
			if(missing(which)){
				if(length(name$files) == 0){
					filename <- tempfile(fileext=".R")
					file.create(filename)
					name <- addFile(name, filename, "/code.R")
					warning("Adding new file to entity named code.R")
				}
				which <- 1:length(name$files)
			}
			if(!(is.numeric(which)))
				stop("argument 'which' must be numeric")
			if(any(which) > length(name$files))
				stop("Invalid file specified")
			
			if(!("file" %in% names(args)))
				file <- file.path(name$cacheDir, name$files[which])
			file.edit(file, ...)
			name
		}
)