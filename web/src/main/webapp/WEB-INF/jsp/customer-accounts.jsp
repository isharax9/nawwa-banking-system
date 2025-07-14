<%-- Page for Admin/Employee to view accounts of a specific customer --%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ include file="/WEB-INF/jspf/_header.jspf" %>
<%-- Set page title for _header.jspf --%>
<c:set var="pageTitle" value="Customer Accounts" scope="request"/>

<div class="info-card">
    <h2>Accounts for Customer: ${customer.name} (ID: ${customer.id})</h2>
    <p class="text-muted">Email: ${customer.email} | Phone: ${customer.phoneNumber}</p>

    <c:if test="${not empty errorMessage}">
        <p class="flash-message error">${errorMessage}</p>
    </c:if>

    <h3 class="mt-4">Bank Accounts</h3>
    <c:choose>
        <c:when test="${not empty accounts}">
            <div class="data-table-container">
                <table class="data-table">
                    <thead>
                    <tr>
                        <th>ID</th>
                        <th>Account No.</th>
                        <th>Type</th>
                        <th>Balance</th>
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
                            <td>
                                <c:choose>
                                    <c:when test="${account.isActive == true}"><span class="text-green">Active</span></c:when>
                                    <c:otherwise><span class="text-red">Inactive</span></c:otherwise>
                                </c:choose>
                            </td>
                            <td>${account.formattedCreatedAt}</td>
                            <td>
                                <div class="action-buttons-group">
                                        <%-- These actions would link to /accounts/manage with pre-selected account --%>
                                    <a href="${pageContext.request.contextPath}/accounts/manage?accountId=${account.id}" class="btn btn-sm btn-outline-primary action-btn">Manage</a>
                                </div>
                            </td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </div>
        </c:when>
        <c:otherwise>
            <p class="text-muted text-center mt-4">No bank accounts found for this customer.</p>
        </c:otherwise>
    </c:choose>

    <p class="text-center mt-3">
        <a href="${pageContext.request.contextPath}/customers/manage" class="btn btn-secondary">Back to Customer List</a>
    </p>
</div>

<%@ include file="/WEB-INF/jspf/_footer.jspf" %>