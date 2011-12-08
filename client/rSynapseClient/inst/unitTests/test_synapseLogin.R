# TODO: Add comment
# 
###############################################################################

unitTestNotLoggedIn <- function(){
    gotException <- FALSE
    tryCatch(createEntity(Dataset(list(name='foo'))), 
             error = function(e) {
                 gotException <- TRUE
                 checkTrue(grepl("please log into Synapse", e))
                 }	)
}
