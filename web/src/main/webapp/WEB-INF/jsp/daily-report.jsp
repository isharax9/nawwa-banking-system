<%-- Daily Banking Report Page (Admin only) --%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ include file="/WEB-INF/jspf/_header.jspf" %>
<%-- Set page title for _header.jspf --%>
<c:set var="pageTitle" value="Daily Report" scope="request"/>

<div class="info-card">
  <h2>Daily Banking Report</h2>
  <p class="text-muted">View system statistics for a specific day. Only accessible by administrators.</p>

  <c:if test="${not empty errorMessage}">
    <p class="flash-message error">${errorMessage}</p>
  </c:if>

  <form action="${pageContext.request.contextPath}/reports/daily" method="get" class="banking-form my-4">
    <div class="form-group">
      <label for="reportDate">Select Report Date:</label>
      <input type="date" id="reportDate" name="reportDate" class="form-control" value="${reportDate}">
      <p class="form-text text-muted">Select a date to view its report. Defaults to yesterday.</p>
    </div>
    <div class="form-group text-center"> <%-- Use text-center for button alignment --%>
      <button type="submit" class="btn btn-primary form-button-fixed-width">View Report</button>
      <%-- Removed Download PDF button --%>
    </div>
  </form>

  <c:if test="${not empty report}">
    <h3 class="mt-4">Report for: <fmt:formatDate value="${report.reportDate}" pattern="yyyy-MM-dd"/></h3>

    <div class="report-section">
      <h4>New Accounts Created:</h4>
      <p>${report.newAccountsCount}</p>
    </div>

    <div class="report-section">
      <h4>Transaction Summary by Type:</h4>
      <div class="data-table-container"> <%-- Wrap table for responsiveness --%>
        <table class="data-table">
          <thead>
          <tr>
            <th>Type</th>
            <th>Count</th>
            <th>Total Amount</th>
          </tr>
          </thead>
          <tbody>
          <c:forEach var="type" items="${report.transactionCountByType.keySet()}">
            <tr>
              <td>${type}</td>
              <td>${report.transactionCountByType.get(type)}</td>
              <td><fmt:formatNumber value="${report.transactionSumByType.get(type)}" type="currency" currencyCode="USD"/></td>
            </tr>
          </c:forEach>
          </tbody>
        </table>
      </div>
    </div>

    <div class="report-section">
      <h4>Accounts with Most Activity (Top 5):</h4>
      <c:choose>
        <c:when test="${not empty report.topAccounts}">
          <div class="data-table-container"> <%-- Wrap table for responsiveness --%>
            <table class="data-table">
              <thead>
              <tr>
                <th>Account Number</th>
                <th>Transactions</th>
              </tr>
              </thead>
              <tbody>
              <c:forEach var="acc" items="${report.topAccounts}">
                <tr>
                  <td>${acc.accountNumber}</td>
                  <td>${acc.transactionCount}</td>
                </tr>
              </c:forEach>
              </tbody>
            </table>
          </div>
        </c:when>
        <c:otherwise>
          <p class="text-muted">No account activity recorded for this period.</p>
        </c:otherwise>
      </c:choose>
    </div>
  </c:if>

  <p class="text-center mt-3"><a href="${pageContext.request.contextPath}/dashboard" class="btn btn-secondary form-button-fixed-width">Back to Dashboard</a></p>
</div>

<%@ include file="/WEB-INF/jspf/_footer.jspf" %>