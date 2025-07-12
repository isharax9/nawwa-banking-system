<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html>
<head>
  <title>Transfer Funds</title>
  <link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/css/style.css">
  <script>
    document.addEventListener('DOMContentLoaded', function() {
      const scheduleCheckbox = document.getElementById('scheduleTransfer');
      const scheduleFields = document.getElementById('scheduleFields');
      const scheduledDateInput = document.getElementById('scheduledDate');
      const scheduledTimeInput = document.getElementById('scheduledTime');

      function updateScheduleFields() {
        if (scheduleCheckbox.checked) {
          scheduleFields.style.display = 'block';
          scheduledDateInput.setAttribute('required', 'required');
          scheduledTimeInput.setAttribute('required', 'required');
        } else {
          scheduleFields.style.display = 'none';
          scheduledDateInput.removeAttribute('required');
          scheduledTimeInput.removeAttribute('required');
          scheduledDateInput.value = '';
          scheduledTimeInput.value = '';
        }
      }

      if ('${param.scheduleTransfer}' === 'on') {
        scheduleCheckbox.checked = true;
      }
      updateScheduleFields();

      scheduleCheckbox.addEventListener('change', updateScheduleFields);
    });
  </script>
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
              ${account.accountNumber} (${account.type} - Balance: <fmt:formatNumber value="${account.balance}" type="currency" currencyCode="USD"/>)
          </option>
        </c:forEach>
      </select>
      <c:if test="${empty accounts}"><p class="form-text text-muted">You have no accounts. Please create one first.</p></c:if>
    </div>

    <div class="form-group">
      <label for="toAccountNumber">To Account Number:</label>
      <input type="text" id="toAccountNumber" name="toAccountNumber" class="form-control" value="${param.toAccountNumber}" required placeholder="e.g., 123456789012">
      <p class="form-text text-muted">(Enter the destination account number, can be another customer's account)</p>
    </div>

    <div class="form-group">
      <label for="amount">Amount:</label>
      <input type="number" id="amount" name="amount" step="0.01" min="0.01" class="form-control" value="${param.amount}" required>
    </div>

    <div class="form-group form-check">
      <input type="checkbox" id="scheduleTransfer" name="scheduleTransfer" class="form-check-input">
      <label class="form-check-label" for="scheduleTransfer">Schedule this transfer?</label>
    </div>

    <div id="scheduleFields" style="display: none;">
      <h3>Schedule Details</h3>
      <div class="form-group">
        <label for="scheduledDate">Scheduled Date:</label>
        <input type="date" id="scheduledDate" name="scheduledDate" class="form-control" value="${param.scheduledDate}" min="${minScheduledDate}">
        <p class="form-text text-muted">Transfers can be scheduled for today or later for testing purposes.</p>
      </div>
      <div class="form-group">
        <label for="scheduledTime">Scheduled Time:</label>
        <input type="time" id="scheduledTime" name="scheduledTime" class="form-control" value="${param.scheduledTime}">
        <p class="form-text text-muted">Time in HH:MM format (e.g., 14:30 for 2:30 PM)</p>
      </div>
    </div>

    <div class="form-group">
      <button type="submit" class="btn btn-primary" <c:if test="${empty accounts}">disabled</c:if>>Process Transfer</button>
    </div>
  </form>

  <p class="text-center"><a href="${pageContext.request.contextPath}/dashboard" class="btn btn-secondary">Back to Dashboard</a></p>
</div>
</body>
</html>