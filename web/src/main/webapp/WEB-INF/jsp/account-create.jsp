<%-- Account creation page --%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ include file="/WEB-INF/jspf/_header.jspf" %>
<%-- Set page title for _header.jspf --%>
<c:set var="pageTitle" value="Create Account" scope="request"/>

<div class="banking-form-container">
    <h2>Create New Bank Account</h2>

    <form action="${pageContext.request.contextPath}/account-create" method="post" class="banking-form">
        <div class="form-group">
            <label for="accountType">Account Type:</label>
            <select id="accountType" name="accountType" class="form-control" required>
                <option value="">-- Select Account Type --</option>
                <c:forEach var="type" items="${accountTypes}">
                    <option value="${type}" <c:if test="${param.accountType == type}">selected</c:if>>${type}</option>
                </c:forEach>
            </select>
        </div>
        <div class="form-group">
            <label for="initialBalance">Initial Deposit Amount:</label>
            <input type="number" id="initialBalance" name="initialBalance" step="0.01" min="0.01" class="form-control" value="${param.initialBalance}" required>
            <p class="form-text text-muted">A minimum of $0.01 is required for initial deposit.</p>
        </div>

        <div class="form-group text-center"> <!-- Center the button -->
            <button type="submit" class="btn btn-primary form-button-fixed-width mb-3">Create Account</button>
        </div>

        <div class="form-group text-center"> <!-- Center the link-button -->
            <a href="${pageContext.request.contextPath}/dashboard" class="btn btn-secondary form-button-fixed-width">Back to Dashboard</a>
        </div>
    </form>
</div>

<%@ include file="/WEB-INF/jspf/_footer.jspf" %>