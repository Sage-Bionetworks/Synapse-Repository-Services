# TODO: Add comment
# 
# Author: furia
###############################################################################


setMethod(
		f = "attach",
		signature = "Layer",
		definition = function (what, pos = 2, name = deparse(substitute(what)), warn.conflicts = TRUE) {
			name <- deparse(substitute(what))
			what <- what@objects
			attach (what, pos = 2, name = name, warn.conflicts = TRUE) 
		}
)
