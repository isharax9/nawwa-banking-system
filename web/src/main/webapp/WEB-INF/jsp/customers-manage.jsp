<%-- Page for Admin/Employee to manage customers --%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ include file="/WEB-INF/jspf/_header.jspf" %>
<%-- Set page title for _header.jspf --%>
<c:set var="pageTitle" value="Manage Customers" scope="request"/>

<div class="info-card">
    <h2>Customer Management</h2>
    <p class="text-muted">View and manage all customer profiles in the system. This feature is for administrators and employees.</p>


    <c:choose>
        <c:when test="${not empty customers}">
            <div class="data-table-container"> <%-- Wrapper for responsive table --%>
                <table class="data-table">
                    <thead>
                    <tr>
                        <th>ID</th>
                        <th>Name</th>
                        <th>Email</th>
                        <th>Phone</th>
                        <th>Address</th>
                        <th>Actions</th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach var="customer" items="${customers}">
                        <tr>
                            <td>${customer.id}</td>
                            <td>${customer.name}</td>
                            <td>${customer.email}</td>
                            <td>${customer.phoneNumber}</td>
                            <td>${customer.address}</td>
                            <td>
                                <div class="action-buttons-group">
                                        <%-- Edit button --%>
                                    <a href="${pageContext.request.contextPath}/customers/edit/${customer.id}" class="btn btn-sm btn-outline-primary action-btn">Edit</a>
                                        <%-- View Accounts button --%>
                                    <a href="${pageContext.request.contextPath}/customers/accounts/${customer.id}" class="btn btn-sm btn-outline-info action-btn">View Accounts</a>
                                        <%-- Add future actions like Delete Customer (with pre-conditions) --%>
                                        <%-- <form action="${pageContext.request.contextPath}/customers/manage" method="post" class="action-form">
                                            <input type="hidden" name="customerId" value="${customer.id}">
                                            <input type="hidden" name="action" value="delete">
                                            <button type="submit" class="btn btn-sm btn-danger action-btn" onclick="return confirm('WARNING: Permanently delete customer ${customer.name}? This cannot be undone and requires zero balance accounts.');">Delete</button>
                                        </form> --%>
                                </div>
                            </td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </div> <%-- END: Wrapper for responsive table --%>
        </c:when>
        <c:otherwise>
            <p class="text-muted text-center mt-4">No customer profiles found in the system.</p>
        </c:otherwise>
    </c:choose>

    <p class="text-center mt-3">
        <a href="${pageContext.request.contextPath}/dashboard" class="btn btn-secondary">Back to Dashboard</a>
    </p>
</div>

<%@ include file="/WEB-INF/jspf/_footer.jspf" %>