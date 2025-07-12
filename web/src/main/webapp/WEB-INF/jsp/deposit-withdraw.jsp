<%--
  Created by IntelliJ IDEA.
  User: ishara
  Date: 2025-07-12
  Time: 11:23 PM
  To change this template use File | Settings | File Templates.
--%>

<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html>
<head>
  <title>Deposit / Withdraw Funds</title>
  <link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>
<div class="header">
  <h1>Welcome, ${loggedInUser.username}!</h1>
  <div class="nav-links">
    <a href="${pageContext.request.contextPath}/dashboard">Dashboard</a>
    <a href="${pageContext.request.contextPath}/account-create">Create Account</a>
    <a href="${pageContext.request.contextPath}/profile-edit">Edit Profile</a>
    <a href="${pageContext.request.contextPath}/transfer">Transfer Funds</a>
    <a href="${pageContext.request.contextPath}/deposit-withdraw">Deposit/Withdraw</a>
    <a href="${pageContext.request.contextPath}/logout?action=confirm">Logout</a>
  </div>
</div>

<div class="content-container">
  <h2>Deposit / Withdraw Funds</h2>

  <c:if test="${not empty errorMessage}">
    <p class="flash-message error">${errorMessage}</p>
  </c:if>

  <form action="${pageContext.request.contextPath}/deposit-withdraw" method="post">
    <div>
      <label for="accountId">Select Account:</label>
      <select id="accountId" name="accountId" required>
        <option value="">-- Choose your account --</option>
        <c:forEach var="account" items="${accounts}">
          <option value="${account.id}" <c:if test="${param.accountId == account.id}">selected</c:if>>
              ${account.accountNumber} (${account.type} - Balance: ${account.balance})
          </option>
        </c:forEach>
      </select>
      <c:if test="${empty accounts}"><p style="color: grey;">You have no accounts. Please create one first.</p></c:if>
    </div>

    <div>
      <label>Transaction Type:</label>
      <c:forEach var="type" items="${transactionTypes}">
        <input type="radio" id="${type.name()}" name="transactionType" value="${type.name()}"
               <c:if test="${param.transactionType == type.name()}">checked</c:if> required>
        <label for="${type.name()}">${type.name()}</label>
      </c:forEach>
    </div>

    <div>
      <label for="amount">Amount:</label>
      <input type="number" id="amount" name="amount" step="0.01" min="0.01" value="${param.amount}" required>
    </div>

    <div>
      <label for="description">Description (Optional):</label>
      <input type="text" id="description" name="description" value="${param.description}">
    </div>

    <div>
      <button type="submit" <c:if test="${empty accounts}">disabled</c:if>>Process Transaction</button>
    </div>
  </form>

  <p><a href="${pageContext.request.contextPath}/dashboard">Back to Dashboard</a></p>
</div>
</body>
</html>
