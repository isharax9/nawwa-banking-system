<%-- Fund transfer page (immediate or scheduled) --%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ include file="/WEB-INF/jspf/_header.jspf" %> <%-- CORRECT HEADER INCLUDE --%>
<%-- Set page title for _header.jspf --%>
<c:set var="pageTitle" value="Transfer Funds" scope="request"/>

<div class="banking-form-container"> <%-- This container is what we want inside main content area --%>
  <h2>Transfer Funds</h2>

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
      <p class="form-text text-muted">Amount must be positive (minimum = $0.01)</p>
    </div>

    <div class="form-group form-check">
      <input type="checkbox" id="scheduleTransfer" name="scheduleTransfer" class="form-check-input">
      <label class="form-check-label" for="scheduleTransfer">Schedule this transfer?</label>
    </div>

    <div id="scheduleFields" class="collapsible-content">
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

    <div class="form-group text-center"> <%-- Use text-center to center the button --%>
      <button type="submit" class="btn btn-primary form-button-fixed-width mb-3" <c:if test="${empty accounts}">disabled</c:if>>Process Transfer</button>
    </div>

    <div class="form-group text-center"> <%-- Use text-center for the back link --%>
      <a href="${pageContext.request.contextPath}/dashboard" class="btn btn-secondary form-button-fixed-width">Back to Dashboard</a>
    </div>
  </form>
</div> <%-- END banking-form-container --%>

<script>
  // Include the JavaScript directly in the JSP or move to a separate JS file
  document.addEventListener('DOMContentLoaded', function() {
    const scheduleCheckbox = document.getElementById('scheduleTransfer');
    const scheduleFields = document.getElementById('scheduleFields');
    const scheduledDateInput = document.getElementById('scheduledDate');
    const scheduledTimeInput = document.getElementById('scheduledTime');

    function toggleScheduleFields(show) {
      if (show) {
        scheduleFields.style.height = scheduleFields.scrollHeight + 'px';
        scheduleFields.style.opacity = '1';
        scheduleFields.style.pointerEvents = 'auto';
        scheduledDateInput.setAttribute('required', 'required');
        scheduledTimeInput.setAttribute('required', 'required');
      } else {
        scheduleFields.style.height = '0';
        scheduleFields.style.opacity = '0';
        scheduleFields.style.pointerEvents = 'none';
        scheduledDateInput.value = '';
        scheduledTimeInput.value = '';
      }
    }

    const initialScheduleChecked = '${param.scheduleTransfer}' === 'on';
    scheduleCheckbox.checked = initialScheduleChecked;
    toggleScheduleFields(initialScheduleChecked);

    scheduleCheckbox.addEventListener('change', function() {
      toggleScheduleFields(this.checked);
    });

    window.addEventListener('resize', function() {
      if (scheduleCheckbox.checked) {
        scheduleFields.style.height = 'auto';
        scheduleFields.style.height = scheduleFields.scrollHeight + 'px';
      }
    });
  });
</script>

<%@ include file="/WEB-INF/jspf/_footer.jspf" %> <%-- CORRECT FOOTER INCLUDE --%>