setMethod(
		f = "edit",
		signature = signature("Code"),
		definition = function(name, which, ...){
			args <- as.list(substitute(list(...)))[-1L]
			if("file" %in% names(args))
				stop("file argument is currently not supported")
			if(missing(which))
				which <- 1:length(name$files)
			if(!(is.numeric(which)))
				stop("argument 'which' must be numeric")
			
			if(!("file" %in% names(args)))
				file <- file.path(name$cacheDir, name$files[which])
			file.edit(file, ...)
			invisible(name)
		}
)