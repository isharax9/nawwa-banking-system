<%-- Change Password page --%>
<%@ include file="/WEB-INF/jspf/_header.jspf" %>
<%-- Set page title for _header.jspf --%>
<c:set var="pageTitle" value="Change Password" scope="request"/>

<div class="banking-form-container">
  <h2>Change Your Password</h2>

  <form action="${pageContext.request.contextPath}/change-password" method="post" class="banking-form">
    <div class="form-group">
      <label for="oldPassword">Current Password:</label>
      <input type="password" id="oldPassword" name="oldPassword" class="form-control" required>
    </div>
    <div class="form-group">
      <label for="newPassword">New Password:</label>
      <input type="password" id="newPassword" name="newPassword" class="form-control" required>
      <p class="form-text text-muted">Password must be at least 8 characters, with uppercase, lowercase, digit, and special character.</p>
    </div>
    <div class="form-group">
      <label for="confirmNewPassword">Confirm New Password:</label>
      <input type="password" id="confirmNewPassword" name="confirmNewPassword" class="form-control" required>
    </div>
    <div class="form-group text-center">
      <button type="submit" class="btn btn-primary w-100">Change Password</button>
    </div>
  </form>

  <p class="text-center mt-3"><a href="${pageContext.request.contextPath}/dashboard" class="btn btn-secondary">Back to Dashboard</a></p>
</div>

<%@ include file="/WEB-INF/jspf/_footer.jspf" %>