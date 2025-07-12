<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%--
  Created by IntelliJ IDEA.
  User: ishara
  Date: 2025-07-12
  Time: 6:00 PM
  To change this template use File | Settings | File Templates.
--%>
<!DOCTYPE html>
<html>
<head>
    <title>Banking System Login</title>
    <link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>
<div class="login-container">
    <h2>Login to Banking System</h2>

    <!-- Display error messages if present -->
    <c:if test="${not empty errorMessage}">
        <p style="color: red;">${errorMessage}</p>
    </c:if>

    <form action="${pageContext.request.contextPath}/login" method="post">
        <div>
            <label for="username">Username:</label>
            <input type="text" id="username" name="username" value="${param.username}" required>
        </div>
        <div>
            <label for="password">Password:</label>
            <input type="password" id="password" name="password" required>
        </div>
        <div>
            <button type="submit">Login</button>
        </div>
    </form>
</div>
</body>
</html>