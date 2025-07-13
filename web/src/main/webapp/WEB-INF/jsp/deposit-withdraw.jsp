<%-- Deposit/Withdrawal transaction page --%>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ include file="/WEB-INF/jspf/_header.jspf" %>
<%-- Set page title for _header.jspf --%>
<c:set var="pageTitle" value="Deposit/Withdraw" scope="request"/>

<div class="banking-form-container">
  <h2>Deposit / Withdraw Funds</h2>

  <form action="${pageContext.request.contextPath}/deposit-withdrawal" method="post" class="banking-form">
    <div class="form-group">
      <label for="accountId">Select Account:</label>
      <select id="accountId" name="accountId" class="form-control" required>
        <option value="">-- Choose your account --</option>
        <c:forEach var="account" items="${accounts}">
          <option value="${account.id}" <c:if test="${param.accountId == account.id}">selected</c:if>>
              ${account.accountNumber} (${account.type} - Balance: <fmt:formatNumber value="${account.balance}" type="currency" currencyCode="USD"/>)
          </option>
        </c:forEach>
      </select>
      <c:if test="${empty accounts}"><p class="form-text text-muted">You have no accounts. Please create one first.</p></c:if>
    </div>

    <div class="form-group">
      <label>Transaction Type:</label>
      <div class="form-check-group"> <%-- New div for radio buttons --%>
        <c:forEach var="type" items="${transactionTypes}">
          <div class="form-check">
            <input type="radio" id="${type.name()}" name="transactionType" value="${type.name()}" class="form-check-input"
                   <c:if test="${param.transactionType == type.name()}">checked</c:if> required>
            <label class="form-check-label" for="${type.name()}">${type.name()}</label>
          </div>
        </c:forEach>
      </div>
    </div>

    <div class="form-group">
      <label for="amount">Amount:</label>
      <input type="number" id="amount" name="amount" step="0.01" min="0.01" class="form-control" value="${param.amount}" required>
      <p class="form-text text-muted">Amount must be positive.</p>
    </div>

    <div class="form-group">
      <label for="description">Description (Optional):</label>
      <input type="text" id="description" name="description" class="form-control" value="${param.description}">
    </div>

    <div class="form-group">
      <button type="submit" class="btn btn-primary w-100" <c:if test="${empty accounts}">disabled</c:if>>Process Transaction</button>
    </div>
  </form>

  <p class="text-center mt-3"><a href="${pageContext.request.contextPath}/dashboard" class="btn btn-secondary">Back to Dashboard</a></p>
</div>

<%@ include file="/WEB-INF/jspf/_footer.jspf" %>