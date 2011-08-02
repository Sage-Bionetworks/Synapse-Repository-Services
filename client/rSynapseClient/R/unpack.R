.unpack <- 
		function(filename)
{
	filename <- path.expand(filename)
	splits <- strsplit(filename, "\\.")
	extension <- tolower(splits[[1]][length(splits[[1]])])
	destdir <- gsub(paste("[\\.]", extension, sep=""), paste("_", .getCache("downloadSuffix"), sep=""), filename)
	
	switch(extension,
		zip = unzip(filename, exdir = destdir),
		gz = untar(filename, exdir = destdir),
		tar = untar(filename, exdir = destdir),
		{ ## default
			splits <- strsplit(filename, .Platform$file.sep)
			destdir <- paste(splits[[1]][-length(splits[[1]])], collapse=.Platform$file.sep)
			attr(filename, "rootDir") <- destdir
			return(filename)
		}
	)	
	files <- list.files(destdir, full.names = TRUE, recursive=TRUE)
	attr(files, "rootDir") <- destdir
	return(files)
}


