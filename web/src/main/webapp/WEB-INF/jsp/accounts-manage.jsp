<%-- Page for Admin/Employee to manage bank accounts --%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ include file="/WEB-INF/jspf/_header.jspf" %>
<%-- Set page title for _header.jspf --%>
<c:set var="pageTitle" value="Manage Accounts" scope="request"/>

<div class="info-card">
  <h2>Bank Account Management</h2>
  <p class="text-muted">View and manage all bank accounts in the system. This feature is for administrators and employees.</p>


  <c:choose>
    <c:when test="${not empty accounts}">
      <table class="data-table">
        <thead>
        <tr>
          <th>ID</th>
          <th>Account No.</th>
          <th>Type</th>
          <th>Balance</th>
          <th>Customer Name</th>
          <th>Status</th>
          <th>Created At</th>
          <th>Actions</th>
        </tr>
        </thead>
        <tbody>
        <c:forEach var="account" items="${accounts}">
          <tr>
            <td>${account.id}</td>
            <td>${account.accountNumber}</td>
            <td>${account.type}</td>
            <td class="text-right"><fmt:formatNumber value="${account.balance}" type="currency" currencyCode="USD"/></td>
            <td>${account.customerName} (${account.customerId})</td>
            <td>
              <c:choose>
                <c:when test="${account.isActive == true}"><span class="text-green">Active</span></c:when>
                <c:otherwise><span class="text-red">Inactive</span></c:otherwise>
              </c:choose>
            </td>
            <td>${account.formattedCreatedAt}</td> <!-- CORRECTED: Use the new formatted getter -->
            <td>
              <div class="action-buttons-group">
                  <%-- Deactivate/Activate --%>
                <c:if test="${account.isActive == true}">
                  <form action="${pageContext.request.contextPath}/accounts/manage" method="post" class="action-form">
                    <input type="hidden" name="accountId" value="${account.id}">
                    <input type="hidden" name="action" value="deactivate">
                    <button type="submit" class="btn btn-sm btn-outline-danger action-btn" onclick="return confirm('Are you sure you want to deactivate account ${account.accountNumber}?');">Deactivate</button>
                  </form>
                </c:if>
                <c:if test="${account.isActive == false}">
                  <form action="${pageContext.request.contextPath}/accounts/manage" method="post" class="action-form">
                    <input type="hidden" name="accountId" value="${account.id}">
                    <input type="hidden" name="action" value="activate">
                    <button type="submit" class="btn btn-sm btn-outline-success action-btn" onclick="return confirm('Are you sure you want to activate account ${account.accountNumber}?');">Activate</button>
                  </form>
                </c:if>

                  <%-- Change Type (Conversion) --%>
                <button type="button" class="btn btn-sm btn-outline-info action-btn" onclick="showChangeTypeModal(${account.id}, '${account.accountNumber}', '${account.type}');">Change Type</button>

                  <%-- Delete (Hard Delete) --%>
                <form action="${pageContext.request.contextPath}/accounts/manage" method="post" class="action-form">
                  <input type="hidden" name="accountId" value="${account.id}">
                  <input type="hidden" name="action" value="delete">
                  <button type="submit" class="btn btn-sm btn-danger action-btn" onclick="return confirm('WARNING: Are you sure you want to PERMANENTLY DELETE account ${account.accountNumber}? This cannot be undone if balance is zero.');">Delete</button>
                </form>
              </div>
            </td>
          </tr>
        </c:forEach>
        </tbody>
      </table>
    </c:when>
    <c:otherwise>
      <p class="text-muted text-center mt-4">No bank accounts found in the system.</p>
    </c:otherwise>
  </c:choose>

  <p class="text-center mt-3">
    <a href="${pageContext.request.contextPath}/dashboard" class="btn btn-secondary">Back to Dashboard</a>
  </p>
</div>

<%-- Modals for pop-up forms (Change Type) --%>
<div id="changeTypeModal" class="modal">
  <div class="modal-content">
    <span class="close-button" onclick="closeModal('changeTypeModal');">×</span>
    <h3>Change Account Type</h3>
    <form action="${pageContext.request.contextPath}/accounts/manage" method="post" class="banking-form">
      <input type="hidden" name="action" value="changeType">
      <input type="hidden" id="modalAccountId" name="accountId">
      <div class="form-group">
        <label for="modalAccountNumber">Account Number:</label>
        <input type="text" id="modalAccountNumber" class="form-control" readonly>
      </div>
      <div class="form-group">
        <label for="modalCurrentType">Current Type:</label>
        <input type="text" id="modalCurrentType" class="form-control" readonly>
      </div>
      <div class="form-group">
        <label for="newAccountType">New Account Type:</label>
        <select id="newAccountType" name="newAccountType" class="form-control" required>
          <option value="">-- Select New Type --</option>
          <c:forEach var="type" items="${accountTypes}">
            <option value="${type}">${type}</option>
          </c:forEach>
        </select>
      </div>
      <div class="form-group">
        <button type="submit" class="btn btn-primary w-100">Change Account Type</button>
      </div>
    </form>
  </div>
</div>

<script>
  // Modal JavaScript (Basic functionality)
  function showChangeTypeModal(accountId, accountNumber, currentType) {
    document.getElementById('modalAccountId').value = accountId;
    document.getElementById('modalAccountNumber').value = accountNumber;
    document.getElementById('modalCurrentType').value = currentType;
    document.getElementById('newAccountType').value = ''; // Reset dropdown
    document.getElementById('changeTypeModal').style.display = 'block';
  }

  function closeModal(modalId) {
    document.getElementById(modalId).style.display = 'none';
  }

  // Close modal if clicked outside content
  window.onclick = function(event) {
    const modal = document.getElementById('changeTypeModal');
    if (event.target == modal) {
      modal.style.display = 'none';
    }
  }
</script>

<%@ include file="/WEB-INF/jspf/_footer.jspf" %>