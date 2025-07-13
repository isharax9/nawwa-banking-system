<%-- Login page for users --%>
<%@ include file="/WEB-INF/jspf/_header.jspf" %>
<%-- Set page title for _header.jspf --%>
<c:set var="pageTitle" value="Login" scope="request"/>

<div class="auth-container">
    <h2>Login to Banking System</h2>

    <form action="${pageContext.request.contextPath}/login" method="post" class="banking-form">
        <div class="form-group">
            <label for="username">Username:</label>
            <input type="text" id="username" name="username" class="form-control" value="${param.username}" required>
        </div>
        <div class="form-group">
            <label for="password">Password:</label>
            <input type="password" id="password" name="password" class="form-control" required>
        </div>
        <div class="form-group">
            <button type="submit" class="btn btn-primary w-100">Login</button>
        </div>
    </form>
    <p class="text-center mt-3">Don't have an account? <a href="${pageContext.request.contextPath}/register">Register here</a></p>
</div>

<%@ include file="/WEB-INF/jspf/_footer.jspf" %>