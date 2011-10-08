# TODO: Add comment
# 
# Author: Matt Furia
###############################################################################

setMethod(
		f = "moveFile",
		signature = "Layer",
		definition = function(entity, src, dest){
			entity@location <- moveFile(entity@location, src, dest)
			entity
		}
)

setMethod(
		f = "moveFile",
		signature = "CachedLocation",
		definition = function(entity, src, dest){
			src <- gsub("^[\\\\/]+","", src)
			dest <- gsub("^[\\\\/]+","", dest)
			if(!(src %in% entity@files))
				stop(sprintf("Invalid file: %s", src))
			
			if(dest %in% entity@files)
				stop(sprintf('Destination file "%s" already exists. Delete it using deleteFile() then try again.', dest))
			
			if(any(grepl(sprintf("^%s/",dest), entity@files)))
				stop(sprintf('Destination file "%s" already exists as a directory. Please choose a different destination filename and try again.', dest))
			
			## if dest is a directory, move but don't rename
			if(grepl("[\\\\/]$", dest) || dest == ""){
				entity <- addFile(entity, file.path(entity@cacheDir, src), dest)
			}else{
				## rename and copy the file to a temp directory, then add it from there
				filename <- gsub("^.+[\\\\/]", "", dest)
				tmpdir <- tempfile()
				dir.create(tmpdir, recursive=TRUE)
				newSrc <- file.path(tmpdir, filename)
				file.copy(file.path(entity@cacheDir, src), newSrc)
				path <- gsub(sprintf("[\\\\/]?%s$",filename),"", dest)
				entity <- addFile(entity, newSrc, path)				
			}
			
			## delete the original file 
			deleteFile(entity, src)
		}			
)


