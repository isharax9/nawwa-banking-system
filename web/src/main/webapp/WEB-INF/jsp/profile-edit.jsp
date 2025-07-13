<%-- User profile editing page --%>
<%@ include file="/WEB-INF/jspf/_header.jspf" %>
<%-- Set page title for _header.jspf --%>
<c:set var="pageTitle" value="Edit Profile" scope="request"/>

<div class="banking-form-container">
    <h2>Edit Your Profile</h2>

    <form action="${pageContext.request.contextPath}/profile-edit" method="post" class="banking-form">
        <div class="form-group">
            <label for="name">Full Name:</label>
            <input type="text" id="name" name="name" class="form-control" value="${customer.name}" required>
        </div>
        <div class="form-group">
            <label for="email">Email (Not Editable):</label>
            <input type="email" id="email" name="email" class="form-control" value="${customer.email}" readonly>
            <p class="form-text text-muted">Email cannot be changed through this form for security reasons.</p>
        </div>
        <div class="form-group">
            <label for="address">Address:</label>
            <input type="text" id="address" name="address" class="form-control" value="${customer.address}" required>
        </div>
        <div class="form-group">
            <label for="phoneNumber">Phone Number:</label>
            <input type="tel" id="phoneNumber" name="phoneNumber" class="form-control" value="${customer.phoneNumber}" required>
            <p class="form-text text-muted">Enter a 10-15 digit phone number (e.g., 1234567890).</p>
        </div>
        <div class="form-group-text-center">
            <button type="submit" class="btn btn-primary form-button-fixed-width mb-3">Update Profile</button>
        </div>
    </form>

    <p class="text-center mt-3"><a href="${pageContext.request.contextPath}/dashboard" class="btn btn-secondary form-button-fixed-width">Back to Dashboard</a></p>
</div>

<%@ include file="/WEB-INF/jspf/_footer.jspf" %>