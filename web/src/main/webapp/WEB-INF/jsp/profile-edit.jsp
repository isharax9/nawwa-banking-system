<%--
  Created by IntelliJ IDEA.
  User: ishara
  Date: 2025-07-12
  Time: 9:48 PM
  To change this template use File | Settings | File Templates.
--%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html>
<head>
    <title>Edit Profile</title>
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
        <a href="${pageContext.request.contextPath}/deposit-withdrawal">Deposit/Withdraw</a>
        <a href="${pageContext.request.contextPath}/logout">Logout</a>
    </div>
</div>

<div class="content-container">
    <h2>Edit Your Profile</h2>

    <c:if test="${not empty errorMessage}">
        <p style="color: red;">${errorMessage}</p>
    </c:if>

    <form action="${pageContext.request.contextPath}/profile-edit" method="post">
        <!-- Hidden field for customer ID if needed, though we use email to fetch -->
        <!-- <input type="hidden" name="customerId" value="${customer.id}"> -->

        <div>
            <label for="name">Full Name:</label>
            <input type="text" id="name" name="name" value="${customer.name}" required>
        </div>
        <div>
            <label for="email">Email (Not Editable):</label>
            <!-- Display email, but make it read-only. This field is for display only. -->
            <input type="email" id="email" name="email" value="${customer.email}" readonly>
        </div>
        <div>
            <label for="address">Address:</label>
            <input type="text" id="address" name="address" value="${customer.address}" required>
        </div>
        <div>
            <label for="phoneNumber">Phone Number:</label>
            <input type="tel" id="phoneNumber" name="phoneNumber" value="${customer.phoneNumber}" required>
        </div>
        <div>
            <button type="submit">Update Profile</button>
        </div>
    </form>

    <p><a href="${pageContext.request.contextPath}/dashboard">Back to Dashboard</a></p>
</div>
</body>
</html>