<%--
  Created by IntelliJ IDEA.
  User: ishara
  Date: 2025-07-12
  Time: 6:32 PM
  To change this template use File | Settings | File Templates.
--%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html>
<head>
    <title>Register for Banking System</title>
    <link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>
<div class="register-container">
    <h2>Register for Banking System</h2>

    <!-- Display error messages if present -->
    <c:if test="${not empty errorMessage}">
        <p style="color: red;">${errorMessage}</p>
    </c:if>

    <form action="${pageContext.request.contextPath}/register" method="post">
        <div>
            <label for="username">Username:</label>
            <input type="text" id="username" name="username" value="${param.username}" required>
        </div>
        <div>
            <label for="password">Password:</label>
            <input type="password" id="password" name="password" required>
        </div>
        <!-- Optionally add email/phone fields if your User entity's register method supports it -->
        <%--
        <div>
            <label for="email">Email:</label>
            <input type="email" id="email" name="email" value="${param.email}" required>
        </div>
        <div>
            <label for="phone">Phone Number:</label>
            <input type="tel" id="phone" name="phone" value="${param.phone}">
        </div>
        --%>
        <div>
            <button type="submit">Register</button>
        </div>
    </form>
    <p>Already have an account? <a href="${pageContext.request.contextPath}/login">Login here</a></p>
</div>
</body>
</html>
