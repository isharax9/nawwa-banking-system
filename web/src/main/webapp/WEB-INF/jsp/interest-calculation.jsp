<%--
  Created by IntelliJ IDEA.
  User: ishara
  Date: 2025-07-13
  Time: 10:59 PM
  To change this template use File | Settings | File Templates.
--%>

<%-- Page for customers to check and apply accrued interest --%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ include file="/WEB-INF/jspf/_header.jspf" %>
<%-- Set page title for _header.jspf --%>
<c:set var="pageTitle" value="Interest Calculation" scope="request"/>

<div class="banking-form-container">
    <h2>Accrued Interest Calculator</h2>
    <p class="text-muted">Select a savings account to view and apply accrued interest.</p>

    <form action="${pageContext.request.contextPath}/interest-calculation" method="get" class="banking-form">
        <div class="form-group">
            <label for="accountId">Select Savings Account:</label>
            <select id="accountId" name="accountId" class="form-control" onchange="this.form.submit()" required>
                <option value="">-- Select your savings account --</option>
                <c:forEach var="account" items="${savingsAccounts}">
                    <option value="${account.id}" <c:if test="${param.accountId == account.id}">selected</c:if>>
                            ${account.accountNumber} (Balance: <fmt:formatNumber value="${account.balance}" type="currency" currencyCode="USD"/>)
                    </option>
                </c:forEach>
            </select>
            <c:if test="${empty savingsAccounts}"><p class="form-text text-muted">You have no savings accounts to calculate interest for.</p></c:if>
        </div>
    </form>

    <c:if test="${not empty selectedAccount}">
        <div class="info-card mt-4 p-4">
            <h3>Interest Details for Account: ${selectedAccount.accountNumber}</h3>
            <p><strong>Current Balance:</strong> <fmt:formatNumber value="${selectedAccount.balance}" type="currency" currencyCode="USD"/></p>
            <p><strong>Last Interest Applied:</strong> ${selectedAccount.formattedLastInterestAppliedDate}</p>
            <p><strong>Accrued Interest:</strong> <fmt:formatNumber value="${accruedInterest}" type="currency" currencyCode="USD"/></p>

            <c:if test="${hasAccruedInterest}">
                <form action="${pageContext.request.contextPath}/interest-calculation" method="post" class="banking-form mt-4">
                    <input type="hidden" name="accountId" value="${selectedAccount.id}">
                    <input type="hidden" name="accruedInterest" value="${accruedInterest}">
                    <p class="form-text text-muted">Clicking "Apply Interest" will add ${accruedInterest} to your account balance.</p>
                    <button type="submit" class="btn btn-primary form-button-fixed-width">Apply Accrued Interest</button>
                </form>
            </c:if>
            <c:if test="${!hasAccruedInterest}">
                <p class="text-muted text-center mt-3">No new interest has accrued or balance is not positive.</p>
            </c:if>
        </div>
    </c:if>

    <p class="text-center mt-3"><a href="${pageContext.request.contextPath}/dashboard" class="btn btn-secondary form-button-fixed-width">Back to Dashboard</a></p>
</div>

<%@ include file="/WEB-INF/jspf/_footer.jspf" %>
