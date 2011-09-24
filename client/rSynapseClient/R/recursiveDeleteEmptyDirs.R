# TODO: Add comment
# 
# Author: furia
###############################################################################


.recursiveDeleteEmptyDirs <-
		function(root, deleteTopLevel=TRUE)
{
	dirs <-setdiff(dir(root, all.files = T, include.dirs = T), c(".", ".."))
	dirs <- dirs[file.info(file.path(root, dirs))$isdir]
	lapply(dirs, function(thisDir) .recursiveDeleteEmptyDirs(file.path(root, thisDir)))

	contents <- setdiff(dir(root, all.files = TRUE, include.dirs=TRUE), c(".", ".."))
	if(length(contents) == 0L && deleteTopLevel)
		unlink(root, recursive=T)
	invisible(NULL)
}

