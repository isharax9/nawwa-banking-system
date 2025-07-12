<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html>
<head>
  <title>User Dashboard</title>
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
      <a href="${pageContext.request.contextPath}/scheduled-transfers">Scheduled Transfers</a> <!-- NEW LINK -->
    </c:if>
    <c:if test="${loggedInUser.hasRole('ADMIN') || loggedInUser.hasRole('EMPLOYEE')}">
      <a href="${pageContext.request.contextPath}/users/manage">Manage Users</a>
      <a href="${pageContext.request.contextPath}/customers/manage">Manage Customers</a>
    </c:if>
    <a href="${pageContext.request.contextPath}/logout?action=confirm">Logout</a>
  </div>
</div>

<div class="content-container">
  <c:if test="${not empty flashMessage}">
    <p class="flash-message ${flashMessageType}">${flashMessage}</p>
  </c:if>
  <c:if test="${not empty errorMessage}">
    <p class="flash-message error">${errorMessage}</p>
  </c:if>

  <c:if test="${loggedInUser.hasRole('CUSTOMER')}">
    <c:if test="${not empty customer}">
      <h2>Hello, ${customer.name}!</h2>
      <p>Email: ${customer.email}</p>
      <p>Address: ${customer.address}</p>
      <p>Phone: ${customer.phoneNumber}</p>
    </c:if>
    <c:if test="${empty customer && not empty errorMessage}">
      <p>Please complete your customer profile setup, or contact support if you believe this is an error.</p>
    </c:if>

    <h2>Your Accounts</h2>
    <c:choose>
      <c:when test="${not empty accounts}">
        <div class="account-list">
          <c:forEach var="account" items="${accounts}">
            <div class="account-card">
              <h3>Account: ${account.accountNumber}</h3>
              <p>Type: ${account.type}</p>
              <p>Balance: <fmt:formatNumber value="${account.balance}" type="currency" currencyCode="USD"/></p>
              <p><a href="${pageContext.request.contextPath}/transactions/account/${account.id}">View Transactions</a></p>
            </div>
          </c:forEach>
        </div>
      </c:when>
      <c:otherwise>
        <p>No accounts found for your customer profile. <a href="${pageContext.request.contextPath}/account-create">Create your first account!</a></p>
      </c:otherwise>
    </c:choose>

    <h2>Recent Transactions</h2>
    <c:choose>
      <c:when test="${not empty recentTransactions}">
        <table class="transaction-table">
          <thead>
          <tr>
            <th>Timestamp</th>
            <th>Account</th>
            <th>Type</th>
            <th>Amount</th>
            <th>Status</th>
            <th>Description</th>
          </tr>
          </thead>
          <tbody>
          <c:forEach var="tx" items="${recentTransactions}">
            <tr>
              <td>${tx.formattedTimestamp}</td>
              <td>${tx.accountNumber}</td>
              <td>${tx.type}</td>
              <td style="color: <c:if test='${tx.amount < 0}'>red</c:if><c:if test='${tx.amount > 0}'>green</c:if>;">
                <fmt:formatNumber value="${tx.amount}" type="currency" currencyCode="USD"/>
              </td>
              <td>${tx.status}</td>
              <td>${tx.description}</td>
            </tr>
          </c:forEach>
          </tbody>
        </table>
      </c:when>
      <c:otherwise>
        <p>No recent transactions to display.</p>
      </c:otherwise>
    </c:choose>
  </c:if>

  <c:if test="${loggedInUser.hasRole('ADMIN') || loggedInUser.hasRole('EMPLOYEE')}">
    <p>${message}</p>
    <h3>Admin/Employee Dashboard Features Coming Soon!</h3>
  </c:if>

</div>
</body>
</html>