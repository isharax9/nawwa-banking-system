<%--
  Created by IntelliJ IDEA.
  User: ishara
  Date: 2025-07-12
  Time: 10:37 PM
  To change this template use File | Settings | File Templates.
--%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html>
<head>
  <title>Confirm Logout</title>
  <link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>
<div class="logout-confirm-container">
  <h2>Confirm Logout</h2>
  <p>Are you sure you want to log out?</p>

  <div class="button-group">
    <form action="${pageContext.request.contextPath}/logout?action=logout" method="post" style="display: inline-block;">
      <button type="submit" class="button button-danger">Yes, Logout</button>
    </form>
    <a href="${pageContext.request.contextPath}/dashboard" class="button button-secondary">Cancel</a>
  </div>
</div>
</body>
</html>
