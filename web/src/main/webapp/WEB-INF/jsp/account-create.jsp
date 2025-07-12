<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html>
<head>
    <title>Create New Account</title>
    <link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>
<div class="header">
    <h1>Welcome, ${loggedInUser.username}!</h1>
    <div class="nav-links">
        <a href="${pageContext.request.contextPath}/dashboard">Dashboard</a>
        <a href="${pageContext.request.contextPath}/account-create">Create Account</a>
        <a href="${pageContext.request.contextPath}/profile-edit">Edit Profile</a>
        <a href="${pageContext.request.contextPath}/transfer">Transfer Funds</a>
        <a href="${pageContext.request.contextPath}/deposit-withdraw">Deposit/Withdraw</a>
        <a href="${pageContext.request.contextPath}/logout?action=confirm">Logout</a>
    </div>
</div>

<div class="content-container">
    <h2>Create New Bank Account</h2>

    <c:if test="${not empty errorMessage}">
        <p class="flash-message error">${errorMessage}</p>
    </c:if>

    <form action="${pageContext.request.contextPath}/account-create" method="post">
        <div>
            <label for="accountType">Account Type:</label>
            <select id="accountType" name="accountType" required>
                <option value="">-- Select Account Type --</option>
                <c:forEach var="type" items="${accountTypes}">
                    <option value="${type}" <c:if test="${param.accountType == type}">selected</c:if>>${type}</option>
                </c:forEach>
            </select>
        </div>
        <div>
            <label for="initialBalance">Initial Deposit Amount:</label>
            <input type="number" id="initialBalance" name="initialBalance" step="0.01" min="0.01" value="${param.initialBalance}" required>
        </div>
        <div>
            <button type="submit">Create Account</button>
        </div>
    </form>

    <p><a href="${pageContext.request.contextPath}/dashboard">Back to Dashboard</a></p>
</div>
</body>
</html>