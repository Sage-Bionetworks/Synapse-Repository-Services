<%@ page isErrorPage="true" %>
<html>
<head>
<title>Internal Server Error</title>
</head>
<body bgcolor="white">
<h3>
Internal Server Error
</h3>
<p>
Request URI: <%= pageContext.getErrorData().getRequestURI() %><p>
Servlet Name: <%= pageContext.getErrorData().getServletName() %><p>
Status Code: <%= pageContext.getErrorData().getStatusCode() %><p>
Error: <%= pageContext.getErrorData().getThrowable() %><p>
Cause: <%= pageContext.getErrorData().getThrowable().getCause() %><p>
</body>
</html> 
