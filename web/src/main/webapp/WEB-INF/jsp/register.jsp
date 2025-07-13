<%-- Registration page for new users --%>
<%@ include file="/WEB-INF/jspf/_header.jspf" %>
<%-- Set page title for _header.jspf --%>
<c:set var="pageTitle" value="Register" scope="request"/>

<div class="auth-container">
    <h2>Register for Banking System</h2>

    <form action="${pageContext.request.contextPath}/register" method="post" class="banking-form">
        <div class="form-group">
            <label for="username">Username:</label>
            <input type="text" id="username" name="username" class="form-control" value="${param.username}" required>
        </div>
        <div class="form-group">
            <label for="password">Password:</label>
            <input type="password" id="password" name="password" class="form-control" required>
            <p class="form-text text-muted">Password must be at least 8 characters, with uppercase, lowercase, digit, and special character.</p>
        </div>
        <div class="form-group">
            <label for="email">Email:</label>
            <input type="email" id="email" name="email" class="form-control" value="${param.email}" required>
        </div>
        <div class="form-group">
            <label for="name">Full Name:</label>
            <input type="text" id="name" name="name" class="form-control" value="${param.name}" required>
        </div>
        <div class="form-group">
            <label for="address">Address:</label>
            <input type="text" id="address" name="address" class="form-control" value="${param.address}" required>
        </div>
        <div class="form-group">
            <label for="phoneNumber">Phone Number:</label>
            <input type="tel" id="phoneNumber" name="phoneNumber" class="form-control" value="${param.phoneNumber}" required>
        </div>
        <div class="form-group">
            <button type="submit" class="btn btn-primary w-100">Register</button>
        </div>
    </form>
    <p class="text-center mt-3">Already have an account? <a href="${pageContext.request.contextPath}/login">Login here</a></p>
</div>

<%@ include file="/WEB-INF/jspf/_footer.jspf" %>