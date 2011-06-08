synapseCacheDir <- function(cacheDir){
	if(missing(cacheDir)){
		return(.getCache("synapseCacheDir"))
	}
	.setCache("synapseCacheDir", cacheDir)
}
