<%-- Scheduled transfers list page --%>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ include file="/WEB-INF/jspf/_header.jspf" %>
<%-- Set page title for _header.jspf --%>
<c:set var="pageTitle" value="Scheduled Transfers" scope="request"/>

<div class="info-card">
  <h2>Your Scheduled Transfers</h2>

  <c:choose>
    <c:when test="${not empty scheduledTransfers}">
      <table class="data-table">
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
            <td>${st.formattedScheduledTime}</td>
            <td>
              <c:choose>
                <c:when test="${st.processed == true}"><span class="text-green">YES</span></c:when>
                <c:otherwise><span class="text-warning">NO</span></c:otherwise>
              </c:choose>
            </td>
            <td>${st.formattedCreatedAt}</td>
          </tr>
        </c:forEach>
        </tbody>
      </table>
    </c:when>
    <c:otherwise>
      <p class="text-muted text-center mt-4">You have no scheduled transfers.</p>
    </c:otherwise>
  </c:choose>

  <p class="text-center mt-3"><a href="${pageContext.request.contextPath}/dashboard" class="btn btn-secondary">Back to Dashboard</a></p>
</div>

<%@ include file="/WEB-INF/jspf/_footer.jspf" %>