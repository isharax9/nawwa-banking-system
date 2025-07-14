<%-- Main dashboard for authenticated users --%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ include file="/WEB-INF/jspf/_header.jspf" %>
<%-- Set page title for _header.jspf --%>
<c:set var="pageTitle" value="Dashboard" scope="request"/>

<div class="info-card">
  <c:if test="${loggedInUser.hasRole('ADMIN') || loggedInUser.hasRole('EMPLOYEE')}">
    <h2 class="mt-4">Admin/Employee Dashboard</h2>
    <p class="lead">${message}</p>
    <p class="text-muted">You have elevated privileges. Use the links below to manage the system.</p>
    <div class="btn-group my-4">
      <a href="${pageContext.request.contextPath}/users/manage" class="btn btn-info">Manage Users</a>
      <a href="${pageContext.request.contextPath}/customers/manage" class="btn btn-info">Manage Customers</a>
      <a href="${pageContext.request.contextPath}/accounts/manage" class="btn btn-info">Manage Bank Accounts</a>
    </div>
  </c:if>
  <c:if test="${loggedInUser.hasRole('CUSTOMER')}">
    <c:if test="${not empty customer}">
      <h2>Hello, ${customer.name}!</h2>
      <p><strong>Email:</strong> ${customer.email}</p>
      <p><strong>Address:</strong> ${customer.address}</p>
      <p><strong>Phone:</strong> ${customer.phoneNumber}</p>
    </c:if>
    <c:if test="${empty customer && not empty errorMessage}">
      <p class="flash-message error">Your customer profile could not be loaded. Please complete your customer profile setup, or contact support if you believe this is an error.</p>
    </c:if>

    <h2>Your Accounts</h2>
    <c:choose>
      <c:when test="${not empty accounts}">
        <div class="account-cards-grid">
          <c:forEach var="account" items="${accounts}">
            <div class="account-card">
              <h3>Account: ${account.accountNumber}</h3>
              <p><strong>Type:</strong> ${account.type}</p>
              <p><strong>Balance:</strong> <fmt:formatNumber value="${account.balance}" type="currency" currencyCode="USD"/></p>
                <%-- REMOVED: <p><strong>Customer:</strong> ${account.customerName}</p> --%>
              <p><strong>Status:</strong>
                <c:choose>
                  <c:when test="${account.isActive == true}"><span class="text-green">Active</span></c:when>
                  <c:otherwise><span class="text-red">Inactive</span></c:otherwise>
                </c:choose>
              </p>
              <p class="text-right mt-3">
                <a href="${pageContext.request.contextPath}/transactions/account/${account.id}" class="btn btn-sm btn-outline-primary">View Transactions</a>
              </p>
            </div>
          </c:forEach>
        </div>
      </c:when>
      <c:otherwise>
        <p class="text-muted text-center mt-4">No accounts found for your customer profile. <a href="${pageContext.request.contextPath}/account-create" class="btn btn-primary btn-sm my-2">Create your first account!</a></p>
      </c:otherwise>
    </c:choose>

    <h2>Recent Transactions</h2>
    <c:choose>
      <c:when test="${not empty recentTransactions}">
        <div class="data-table-container">
          <table class="data-table">
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
                <td class="<c:if test='${tx.amount < 0}'>text-red</c:if><c:if test='${tx.amount > 0}'>text-green</c:if>">
                  <fmt:formatNumber value="${tx.amount}" type="currency" currencyCode="USD"/>
                </td>
                <td>${tx.status}</td>
                <td>${tx.description}</td>
              </tr>
            </c:forEach>
            </tbody>
          </table>
        </div>
      </c:when>
      <c:otherwise>
        <p class="text-muted text-center mt-4">No recent transactions to display.</p>
      </c:otherwise>
    </c:choose>
  </c:if>



</div>

<%@ include file="/WEB-INF/jspf/_footer.jspf" %>