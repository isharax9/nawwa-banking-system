<%--
  Created by IntelliJ IDEA.
  User: ishara
  Date: 2025-07-13
  Time: 5:25 AM
  To change this template use File | Settings | File Templates.
--%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html>
<head>
  <title>Change Password</title>
  <link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>
<div class="header">
  <h1>Welcome, ${loggedInUser.username}!</h1>
  <div class="nav-links">
    <a href="${pageContext.request.contextPath}/dashboard">Dashboard</a>
    <c:if test="${loggedInUser.hasRole('CUSTOMER')}">
      <a href="${pageContext.request.contextPath}/account-create">Create Account</a>
      <a href="${pageContext.request.contextPath}/profile-edit">Edit Profile</a>
      <a href="${pageContext.request.contextPath}/transfer">Transfer Funds</a>
      <a href="${pageContext.request.contextPath}/deposit-withdraw">Deposit/Withdraw</a>
      <a href="${pageContext.request.contextPath}/scheduled-transfers">Scheduled Transfers</a>
    </c:if>
    <c:if test="${loggedInUser.hasRole('ADMIN') || loggedInUser.hasRole('EMPLOYEE')}">
      <a href="${pageContext.request.contextPath}/users/manage">Manage Users</a>
      <a href="${pageContext.request.contextPath}/customers/manage">Manage Customers</a>
    </c:if>
    <a href="${pageContext.request.contextPath}/logout?action=confirm">Logout</a>
  </div>
</div>

<div class="content-container">
  <h2>Change Your Password</h2>

  <c:if test="${not empty errorMessage}">
    <p class="flash-message error">${errorMessage}</p>
  </c:if>

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
    <div>
      <button type="submit" class="btn btn-primary">Change Password</button>
    </div>
  </form>

  <p class="text-center"><a href="${pageContext.request.contextPath}/dashboard" class="btn btn-secondary">Back to Dashboard</a></p>
</div>
</body>
</html>
