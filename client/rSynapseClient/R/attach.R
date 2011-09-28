# TODO: Add comment
# 
# Author: furia
###############################################################################

setMethod(
		f = "attach",
		signature = "Layer",
		definition = function (what, pos = 2, name, warn.conflicts = TRUE) {
			if(missing(name))
				name = getPackageName(what@objects)
			
			what <- what@objects
			attach (what, pos = pos, name = name, warn.conflicts) 
		}
)
