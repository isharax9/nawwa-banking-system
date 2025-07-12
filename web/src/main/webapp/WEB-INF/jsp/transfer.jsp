<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html>
<head>
  <title>Transfer Funds</title>
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
    <a href="${pageContext.request.contextPath}/deposit-withdrawal">Deposit/Withdraw</a>
    <a href="${pageContext.request.contextPath}/logout?action=confirm">Logout</a>
  </div>
</div>

<div class="content-container">
  <h2>Transfer Funds</h2>

  <c:if test="${not empty errorMessage}">
    <p class="flash-message error">${errorMessage}</p>
  </c:if>

  <form action="${pageContext.request.contextPath}/transfer" method="post" class="banking-form">
    <div class="form-group">
      <label for="fromAccountId">From Account:</label>
      <select id="fromAccountId" name="fromAccountId" class="form-control" required>
        <option value="">-- Select your account --</option>
        <c:forEach var="account" items="${accounts}">
          <option value="${account.id}" <c:if test="${param.fromAccountId == account.id}">selected</c:if>>
              ${account.accountNumber} (${account.type} - Balance: ${account.balance})
          </option>
        </c:forEach>
      </select>
      <c:if test="${empty accounts}"><p class="form-text text-muted">You have no accounts. Please create one first.</p></c:if>
    </div>

    <div class="form-group">
      <label for="toAccountNumber">To Account Number:</label>
      <!-- Changed to text input for any account number -->
      <input type="text" id="toAccountNumber" name="toAccountNumber" class="form-control" value="${param.toAccountNumber}" required placeholder="e.g., 123456789012">
      <p class="form-text text-muted">(Enter the destination account number, can be another customer's account)</p>
    </div>

    <div class="form-group">
      <label for="amount">Amount:</label>
      <input type="number" id="amount" name="amount" step="0.01" min="0.01" class="form-control" value="${param.amount}" required>
    </div>

    <div class="form-group">
      <button type="submit" class="btn btn-primary" <c:if test="${empty accounts}">disabled</c:if>>Transfer Funds</button>
    </div>
  </form>

  <p class="text-center"><a href="${pageContext.request.contextPath}/dashboard" class="btn btn-secondary">Back to Dashboard</a></p>
</div>
</body>
</html>