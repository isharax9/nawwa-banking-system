<%-- This is the public home page --%>
<%@ include file="/WEB-INF/jspf/_header.jspf" %>
<%-- Set page title for _header.jspf --%>
<c:set var="pageTitle" value="Welcome" scope="request"/>

<div class="auth-container">
  <h1>Welcome to Your Secure Banking System!</h1>
  <p>Your trusted partner for all banking needs.</p>
  <div class="btn-group my-4">
    <a href="${pageContext.request.contextPath}/login" class="btn btn-primary">Login</a>
    <a href="${pageContext.request.contextPath}/register" class="btn btn-secondary">Register</a>
  </div>
  <p class="text-muted mt-3">Manage your finances with ease and security.</p>
</div>

<%@ include file="/WEB-INF/jspf/_footer.jspf" %>