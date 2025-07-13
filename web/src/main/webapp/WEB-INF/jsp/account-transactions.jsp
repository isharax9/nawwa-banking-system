<%-- Account transaction history page --%>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ include file="/WEB-INF/jspf/_header.jspf" %>
<%-- Set page title for _header.jspf --%>
<c:set var="pageTitle" value="Account History" scope="request"/>

<div class="info-card">
    <h2 class="mb-3">Transaction History for Account: ${account.accountNumber}</h2>
    <p><strong>Account Type:</strong> ${account.type}</p>
    <p><strong>Current Balance:</strong> <fmt:formatNumber value="${account.balance}" type="currency" currencyCode="USD"/></p>
    <p class="text-muted">Account ID: ${account.id}</p>

    <h3 class="mt-4">All Transactions</h3>
    <c:choose>
        <c:when test="${not empty transactions}">
            <table class="data-table">
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
                        <td class="<c:if test='${tx.amount < 0}'>text-red</c:if><c:if test='${tx.amount > 0}'>text-green</c:if>">
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
            <p class="text-muted text-center mt-4">No transactions found for this account.</p>
        </c:otherwise>
    </c:choose>

    <p class="text-center mt-3"><a href="${pageContext.request.contextPath}/dashboard" class="btn btn-secondary">Back to Dashboard</a></p>
</div>

<%@ include file="/WEB-INF/jspf/_footer.jspf" %>