<%-- Page for Admin/Employee to manage users --%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ include file="/WEB-INF/jspf/_header.jspf" %>
<%-- Set page title for _header.jspf --%>
<c:set var="pageTitle" value="Manage Users" scope="request"/>

<div class="info-card">
  <h2>User Management</h2>
  <p class="text-muted">View and manage all system users. This feature is for administrators and employees.</p>

  <c:if test="${not empty flashMessage}">
    <p class="flash-message ${flashMessageType}">${flashMessage}</p>
  </c:if>
  <c:if test="${not empty errorMessage}">
    <p class="flash-message error">${errorMessage}</p>
  </c:if>

  <c:choose>
    <c:when test="${not empty users}">
      <table class="data-table">
        <thead>
        <tr>
          <th>ID</th>
          <th>Username</th>
          <th>Email</th>
          <th>Roles</th>
          <th>Status</th>
          <th>Created At</th>
          <th>Actions</th>
        </tr>
        </thead>
        <tbody>
        <c:forEach var="user" items="${users}">
          <tr>
            <td>${user.id}</td>
            <td>${user.username}</td>
            <td>${user.email}</td>
            <td>
              <c:forEach var="role" items="${user.roles}" varStatus="loop">
                ${role.name()}<c:if test="${!loop.last}">, </c:if>
              </c:forEach>
            </td>
            <td>
              <c:choose>
                <c:when test="${user.isActive == true}"><span class="text-green">Active</span></c:when>
                <c:otherwise><span class="text-red">Inactive</span></c:otherwise>
              </c:choose>
            </td>
            <td>${user.formattedCreatedAt}</td> <!-- CORRECTED: Use the new formatted getter -->
            <td>
                <%-- Action links (will be implemented later) --%>
              <a href="#" class="btn btn-sm btn-outline-primary">Edit</a>
              <c:if test="${user.isActive == true}">
                <a href="#" class="btn btn-sm btn-outline-secondary">Deactivate</a>
              </c:if>
              <c:if test="${user.isActive == false}">
                <a href="#" class="btn btn-sm btn-outline-success">Activate</a>
              </c:if>
            </td>
          </tr>
        </c:forEach>
        </tbody>
      </table>
    </c:when>
    <c:otherwise>
      <p class="text-muted text-center mt-4">No users found in the system.</p>
    </c:otherwise>
  </c:choose>

  <p class="text-center mt-3">
    <a href="${pageContext.request.contextPath}/dashboard" class="btn btn-secondary">Back to Dashboard</a>
    <%-- <a href="${pageContext.request.contextPath}/users/create" class="btn btn-primary">Create New User</a> --%>
  </p>
</div>

<%@ include file="/WEB-INF/jspf/_footer.jspf" %>