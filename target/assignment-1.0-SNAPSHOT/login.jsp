<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Login</title>
    </head>
    <body>
        <div>
            <form method="post" action="login">
                <div>
                    <label for="username"> Username:</label>
                    <input type="text" name="username" id="username"
                           required="required" />
                    <div>
                        <label for="password"> Password:</label>
                        <input type="password" name="password" id="password"
                               required="required" />
                    </div>
                    <c:if test="${not empty requestScope.errorMessage}">
                        <div class="error-message">
                            <c:out value="${requestScope.errorMessage}" />
                        </div>
                    </c:if>
                    <div>
                        <input type="submit" value="Login" />
                    </div>


            </form>
        </div>
    </body>
</html>