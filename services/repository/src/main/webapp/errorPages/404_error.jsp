<%@ page isErrorPage="true" %>
<html>
<head>
<title>Not Found</title>
</head>
<body bgcolor="white">
<h3>
Not Found
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
