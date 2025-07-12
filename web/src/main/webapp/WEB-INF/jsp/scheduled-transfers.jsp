<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html>
<head>
  <title>Scheduled Transfers</title>
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
  <h2>Your Scheduled Transfers</h2>

  <c:if test="${not empty flashMessage}">
    <p class="flash-message ${flashMessageType}">${flashMessage}</p>
  </c:if>
  <c:if test="${not empty errorMessage}">
    <p class="flash-message error">${errorMessage}</p>
  </c:if>

  <c:choose>
    <c:when test="${not empty scheduledTransfers}">
      <table class="transaction-table">
        <thead>
        <tr>
          <th>ID</th>
          <th>From Account</th>
          <th>To Account</th>
          <th>Amount</th>
          <th>Scheduled Time</th>
          <th>Processed</th>
          <th>Created At</th>
        </tr>
        </thead>
        <tbody>
        <c:forEach var="st" items="${scheduledTransfers}">
          <tr>
            <td>${st.id}</td>
            <td>${st.fromAccount.accountNumber}</td>
            <td>${st.toAccount.accountNumber}</td>
            <td><fmt:formatNumber value="${st.amount}" type="currency" currencyCode="USD"/></td>
            <td>${st.formattedScheduledTime}</td> <!-- CORRECTED -->
            <td>
              <c:choose>
                <c:when test="${st.processed == true}"><span style="color: green; font-weight: bold;">YES</span></c:when>
                <c:otherwise><span style="color: orange; font-weight: bold;">NO</span></c:otherwise>
              </c:choose>
            </td>
            <td>${st.formattedCreatedAt}</td> <!-- CORRECTED -->
          </tr>
        </c:forEach>
        </tbody>
      </table>
    </c:when>
    <c:otherwise>
      <p>You have no scheduled transfers.</p>
    </c:otherwise>
  </c:choose>

  <p class="text-center"><a href="${pageContext.request.contextPath}/dashboard" class="btn btn-secondary">Back to Dashboard</a></p>
</div>
</body>
</html>