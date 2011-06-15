.unpack <- 
		function(filename, destdir)
{
	splits <- strsplit(filename, "\\.")
	extension <- tolower(splits[[1]][length(splits[[1]])])
	
	switch(extension,
		zip = unzip(filename, exdir = destdir),
		gz = untar(filename, exdir = destdir),
		tar = untar(filename, exdir = destdir),
		defult = stop("unsupported file extension: ", extension)
	)	
	files <- list.files(destdir, full.names = TRUE, recursive=T)
}


