dataLocationPrefs <- function(locations){
	if(missing(locations)){
		return(.getCache("dataLocationPrefs"))
	}
	#if(!all(dataLocationPrefs %in% .getCache("supportedRepositoryLocationTypes"))){
		#ind <- which(!(locations %in% .getCache("supportedRepositoryLocationTypes")))
		#stop(paste("unsupported repository location(s):", locations[ind]))
	#}
	.setCache("dataLocationPrefs", locations)
}
