<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html>
<head>
    <title>Access Denied</title>
    <%-- Include your common CSS/styles here --%>
    <link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/resources/css/style.css"> <%-- Example CSS path --%>
    <style>
        body { font-family: sans-serif; padding: 20px; }
        .error-message { color: red; border: 1px solid red; padding: 10px; margin-bottom: 15px; }
        .container { max-width: 600px; margin: auto; background-color: #f9f9f9; padding: 20px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
        h1 { color: #dc3545; }
    </style>
</head>
<body>
    <div class="container">
        <h1>Access Denied</h1>
        <p class="error-message">
            <%
                String errorMessage = (String) request.getAttribute("errorMessage");
                if (errorMessage == null || errorMessage.isEmpty()) {
                    out.print("Sorry, you do not have the necessary permissions to access this page.");
                } else {
                    out.print(request.getAttribute("errorMessage"));
                }
            %>
        </p>
        <p>Please contact the system administrator if you believe this is an error.</p>
        <p><a href="${pageContext.request.contextPath}/welcome.jsp">Return to Welcome Page</a></p> <%-- Or link to login page if appropriate --%>
         <p><a href="${pageContext.request.contextPath}/login?action=logout">Logout</a></p>
    </div>
</body>
</html>