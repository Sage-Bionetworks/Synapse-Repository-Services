<%@ page isErrorPage="true" %>
<html>
<head>
<title>Server Error</title>
</head>
<body bgcolor="white">
<h3>
Server Error
</h3>
<p>
<% if (null != pageContext.getErrorData()) { %>
  Request URI: <%= pageContext.getErrorData().getRequestURI() %><p>
  Servlet Name: <%= pageContext.getErrorData().getServletName() %><p>
  Status Code: <%= pageContext.getErrorData().getStatusCode() %><p>
  <% if (null != pageContext.getErrorData().getThrowable()) { %>
    Error: <%= pageContext.getErrorData().getThrowable() %><p>
    Cause: <%= pageContext.getErrorData().getThrowable().getCause() %><p>
  <% } %>
<% } %>
</body>
</html> 
