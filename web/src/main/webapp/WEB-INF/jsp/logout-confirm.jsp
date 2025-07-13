<%-- Logout confirmation page --%>
<%@ include file="/WEB-INF/jspf/_header.jspf" %>
<%-- Set page title for _header.jspf --%>
<c:set var="pageTitle" value="Confirm Logout" scope="request"/>

<div class="auth-container">
  <h2>Confirm Logout</h2>
  <p class="lead">Are you sure you want to log out?</p>

  <div class="btn-group my-4">
    <form action="${pageContext.request.contextPath}/logout?action=logout" method="post" style="display: inline-block;">
      <button type="submit" class="btn btn-danger">Yes, Logout</button>
    </form>
    <a href="${pageContext.request.contextPath}/dashboard" class="btn btn-secondary">Cancel</a>
  </div>
</div>

<%@ include file="/WEB-INF/jspf/_footer.jspf" %>