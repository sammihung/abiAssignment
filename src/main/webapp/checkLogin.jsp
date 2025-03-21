<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%
    if (session.getAttribute("userInfo") == null) {
        response.sendRedirect(request.getContextPath()+ "/");
        return;
    }
%>