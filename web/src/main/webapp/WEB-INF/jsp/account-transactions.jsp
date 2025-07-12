<%--
  Created by IntelliJ IDEA.
  User: ishara
  Date: 2025-07-13
  Time: 4:49 AM
  To change this template use File | Settings | File Templates.
--%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html>
<head>
    <title>Transaction History for Account ${account.accountNumber}</title>
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
            <a href="${pageContext.request.contextPath}/deposit-withdrawal">Deposit/Withdraw</a>
        </c:if>
        <c:if test="${loggedInUser.hasRole('ADMIN') || loggedInUser.hasRole('EMPLOYEE')}">
            <a href="${pageContext.request.contextPath}/users/manage">Manage Users</a>
            <a href="${pageContext.request.contextPath}/customers/manage">Manage Customers</a>
        </c:if>
        <a href="${pageContext.request.contextPath}/logout?action=confirm">Logout</a>
    </div>
</div>

<div class="content-container">
    <h2>Transaction History for Account: ${account.accountNumber}</h2>
    <p><strong>Account Type:</strong> ${account.type}</p>
    <p><strong>Current Balance:</strong> <fmt:formatNumber value="${account.balance}" type="currency" currencyCode="USD"/></p>
    <p><strong>Account ID:</strong> ${account.id}</p>

    <c:if test="${not empty errorMessage}">
        <p class="flash-message error">${errorMessage}</p>
    </c:if>

    <h3>All Transactions</h3>
    <c:choose>
        <c:when test="${not empty transactions}">
            <table class="transaction-table">
                <thead>
                <tr>
                    <th>Timestamp</th>
                    <th>Type</th>
                    <th>Amount</th>
                    <th>Status</th>
                    <th>Description</th>
                </tr>
                </thead>
                <tbody>
                <c:forEach var="tx" items="${transactions}">
                    <tr>
                        <td>${tx.formattedTimestamp}</td>
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
            <p>No transactions found for this account.</p>
        </c:otherwise>
    </c:choose>

    <p class="text-center"><a href="${pageContext.request.contextPath}/dashboard" class="btn btn-secondary">Back to Dashboard</a></p>
</div>
</body>
</html>