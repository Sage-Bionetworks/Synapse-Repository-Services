// The file-service endpoint
var FILE_SERVICE_ENDPOINT = "http://localhost:8080/services-file/file/v1";
var SESSION_TOKEN_COOKIE_NAME = "org.sagbionetworks.security.user.login.token";
var SESSION_TOKEN_HEADER_NAME = "sessionToken";


// Create an
function createCORSRequest() {
	var xhr = new XMLHttpRequest();
	// This test is from http://blogs.msdn.com/b/ie/archive/2012/02/09/cors-for-xhr-in-ie10.aspx
	if ("withCredentials" in xhr) {
		return xhr;
	} else {
		// Fallback behavior for browsers without CORS for XHR
		alert("This browser does not support Cross-Orign Resource Sharing (CORS)");
	}
}

// Get a cookie by name
function getCookie(c_name) {
	var i, x, y, ARRcookies = document.cookie.split(";");
	for (i = 0; i < ARRcookies.length; i++) {
		x = ARRcookies[i].substr(0, ARRcookies[i].indexOf("="));
		y = ARRcookies[i].substr(ARRcookies[i].indexOf("=") + 1);
		x = x.replace(/^\s+|\s+$/g, "");
		if (x == c_name) {
			return unescape(y);
		}
	}
}

// Upload a file
function uploadFile(file){
	// Keep track of the file we are uploading
	var fileToUpload = file;
	// This will keep track of the parts we create.
	var chunkParts.list = new Array();
	var chunkedFileToken;
	
	// We are now ready to start the chain of events to upload a file
	this.startChunkedFileUploadToken(this.fileToUpload.name, this.fileToUpload.type);
	
	// Start a chunked file upload
	function startChunkedFileUploadToken(fileName, contentType){
		var xhr = createCORSRequest();
		// only continue if we have a request.
		if(xhr){
			var createRequest = new Object();
			createRequest.fileName = fileName;
			createRequest.contentType = contentType;
			xhr.addEventListener("load", callbackTokenCreated, false);
			xhr.addEventListener("error", callbackFailed, false);
			xhr.addEventListener("abort", callbackCanceled, false);
			xhr.open('POST',FILE_SERVICE_ENDPOINT+"/createChunkedFileUploadToken, true);
			// Set the session token from the cookie.
			xhr.setRequestHeader(SESSION_TOKEN_HEADER_NAME, getCookie(SESSION_TOKEN_COOKIE_NAME));
			xhr.setRequestHeader("Accept", "application/json");
			xhr.send(createRequest);
		}
	}
	
	// Called once we have a chunked file token.
	function callbackTokenCreated(evt){
		this.chunkedFileToken = JSON.parse(this.responseText);
		// Next step is to start uploading the file
		startFileUpload(chunkedFileToken);
	}
	
	function startFileUpload(chunkedFileToken){
		// For this first version we upload the file as a single chunk
		createPreSignedURL(chunkedFileToken, 1, callbackPresignedCreated);
	}
	
	function createPreSignedURL(chunkedFileToken, partNumber, callbackPresignedCreated){
		var partRequest = new Object();
		partRequest.chunkedFileToken = chunkedFileToken;
		partRequest.partNumber = 1;
		// We can now use the token to create our 
		var xhr = createCORSRequest();
		// only continue if we have a request.
		if(xhr){
			xhr.addEventListener("load", callbackPresignedCreated, false);
			xhr.addEventListener("error", callbackFailed, false);
			xhr.addEventListener("abort", callbackCanceled, false);
			xhr.open('POST',FILE_SERVICE_ENDPOINT+"/createChunkedFileUploadChunkURL, true);
			// Set the session token from the cookie.
			xhr.setRequestHeader(SESSION_TOKEN_HEADER_NAME, getCookie(SESSION_TOKEN_COOKIE_NAME));
			xhr.setRequestHeader("Accept", "text/plain");
			xhr.send(partRequest);
		}
	}
	
	public callbackPresignedCreated(evt){
		alert(this.responseText);
	}
	
	// Generic callback for failures
	function callbackFailed(evt) {
		alert("There was an error attempting to upload the file.");
	}
	// Generic callback for cancel
	function callbackCanceled(evt) {
		alert("The upload has been canceled by the user or the browser dropped the connection.");
	}
}




function createChunkURL(chunkedFileToken, partNumber){
	var xhr = createCORSRequest();
	// only continue if we have a request.
	if(xhr){
		xhr.addEventListener("load", chunkedTokenCreated, false);
		xhr.addEventListener("error", callbackFailed, false);
		xhr.addEventListener("abort", callbackCanceled, false);
		xhr.open('POST',FILE_SERVICE_ENDPOINT+"/chunkedFileUploadToken?partNumber="+ partNumber, true);
		// Set the session token from the cookie.
		xhr.setRequestHeader(SESSION_TOKEN_HEADER_NAME, getCookie(SESSION_TOKEN_COOKIE_NAME));
		xhr.setRequestHeader("Accept", "text/plain");
		xhr.send(chunkedFileToken);
	}
}

