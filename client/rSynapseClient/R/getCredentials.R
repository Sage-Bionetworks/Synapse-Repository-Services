getCredentials <- function(username){
	
	## in unix systems, get the credentials from the terminal
	## unless called from a gui. then, first check to see if the
	## system has a working tcl/tk installation
	if(tolower(.Platform$OS.type) == "unix"){
		if(!is.null(.getCache("useTk"))){
			if(.getCache("useTk")){
				return(.tkGetCredentials(username))
			}else{
				return(.terminalGetCredentials(username))
			}
		}
		
		## By default don't check for tk with the CRAN GUI since it hangs when tk isn't properly installed
		if(tolower(.Platform$GUI) == "aqua")
			return(.terminalGetCredentials(username))
		
		if(tolower(.Platform$GUI) != "x11"){
			if(.hasTk())
				return(.tkGetCredentials(username))	
		}
		return(.terminalGetCredentials(username))
	}else if(tolower(.Platform$OS.type) == "windows"){
		## if the called from a GUI get the credentials from a TK widget
		if(.hasTk())
			return(.tkGetCredentials(username))
		return(.terminalGetCredentials(username))
	}
	
	## for platforms other than unix and windows, don't use tk
	return(.terminalGetCredentials(username))
}


.tkGetCredentials <- function(username){
	title = "Synapse Login"
	entryWidth=35
			
	credentials <- NULL
	
	## tk doesn't like NULLs
	if(is.null(title))
		title <- ""
	if(is.null(username))
		username <- ""

	## set up toplevel widget
	root <- tcltk::tktoplevel()
	tcltk::tktitle(root) <- title
	
	## set up variables to catch username and password
	## initialize username to the passed value
	userNameVar <- tcltk::tclVar(username)
	passwordVar <- tcltk::tclVar("")
	
	## Text Entry Widgets for username and password. The password entry hides input
	usernameEntryWidget <- tcltk::tkentry(root, width = entryWidth, textvariable = userNameVar)
	passwordEntryWidget <- tcltk::tkentry(root, width = entryWidth, textvariable = passwordVar, show = "*")
	
	## Event handlers for OK and Cancel buttons
	onLogin <- function()
	{
		credentials$username <<- tcltk::tclvalue(userNameVar)
		credentials$password <<- tcltk::tclvalue(passwordVar)
		tcltk::tkgrab.release(root)
		tcltk::tkdestroy(root)
	}
	onCancel <- function()
	{
		credentials <<- NULL
		tcltk::tkgrab.release(root)
		tcltk::tkdestroy(root)
	}
	
	## OK and Cancel buttons
	loginButton <- tcltk::tkbutton(root,text=" Login  ", command=onLogin)
	cancelButton <- tcltk::tkbutton(root,text=" Cancel ", command=onCancel)
	
	## the first row is for username
	tcltk::tkgrid(tcltk::tklabel(root,text="Username"), column=0, row=0)
	tcltk::tkgrid(usernameEntryWidget, column=1, row=0, columnspan=2)
	
	## the second row is for password
	tcltk::tkgrid(tcltk::tklabel(root, text="Password"), column=0, row=1)
	tcltk::tkgrid(passwordEntryWidget, column=1, row=1, columnspan=2)
	
	## the third row is for the Login and Cancel buttons
	tcltk::tkgrid(cancelButton, column=1, row=2)
	tcltk::tkgrid(loginButton, column=2, row=2)
	
	## bind the return key to onLogin function when in the passwordEntry widget and to
	## set focus to the password entry widget when in the ussername entry widget
	tcltk::tkbind(usernameEntryWidget, "<Return>", function(){tcltk::tkfocus(passwordEntryWidget)})
	tcltk::tkbind(passwordEntryWidget, "<Return>", onLogin)
	
	## set the focus to the root widget
	tcltk::tkgrab.set(root)
	if(username == ""){
		tcltk::tkfocus(usernameEntryWidget)
	}else{
		tcltk::tkfocus(passwordEntryWidget)
	}
	
	## bind the destroy method
	tcltk::tkbind(root, "<Destroy>", function() {tcltk::tkgrab.release(root)})
	
	## wait for user input
	tcltk::tkwait.window(root)
	
	## return the credentials
	credentials
}

.terminalGetCredentials <- function(username){
	credentials <- NULL
	credentials$username <- username
	
	if(username == "")
		credentials$username <- .getUsername()
	credentials$password <- .getPassword()
	
	credentials
}

.getUsername <- function(){
	readline(prompt="Username: ")
}

.getPassword <- function(){
	## Currently only suppresses output in unix-like terminals
	
	finallyCmd <- NULL
	if(tolower(.Platform$GUI) == "x11"){
		if(tolower(.Platform$OS.type) == "unix"){
			## this is a unix terminal
			system("stty -echo")
			finallyCmd <- "stty echo"
		}
	}else if(tolower(.Platform$GUI) == "rterm"){
		if(tolower(.Platform$OS.type) == "windows"){
			## this is a windows terminal
			## TODO figure out how to suppress terminal output in Windows
		}
	}
	
	tryCatch(
			password <- readline(prompt="Password: "),
			finally={
				if(!is.null(finallyCmd)){
					system(finallyCmd) ## turn echo back on only if it was turned off
					cat("\n")
				}
			}
	)
	return(password)
}

.hasTk <- function(){
	if(is.null(.getCache("useTk"))){
		## check to see if the system has a working tk installation
		origWarn <- options()$warn
		options(warn=-1)
		tryCatch({
					tcltk::tktoplevel()
					.setCache("useTk", TRUE)
				},
				error = function(e){
					msg <- "tcl/tk does not seem to be installed on your system. The GUI login widget has been disabled."
					warning(msg, .call=FALSE)
					.setCache("useTk", FALSE)
				},
				warning = function(e){
					msg <- "tcl/tk does not seem to be installed on your system. The GUI login widget has been disabled."
					warning(msg, .call=FALSE)
					.setCache("useTk", FALSE)
				},
				finally={
					
					options(warn=origWarn)
				}
		)
	}
	.getCache("useTk")
}
