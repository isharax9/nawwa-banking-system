<%-- Page for Admin/Employee to manage users --%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ include file="/WEB-INF/jspf/_header.jspf" %>
<%-- Set page title for _header.jspf --%>
<c:set var="pageTitle" value="Manage Users" scope="request"/>

<div class="info-card">
  <h2>User Management</h2>
  <p class="text-muted">View and manage all system users. This feature is for administrators and employees.</p>



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
            <td>${user.formattedCreatedAt}</td>
            <td>
              <div class="action-buttons-group"> <%-- New div for button alignment --%>
                  <%-- Edit button (placeholder) --%>


                  <%-- Deactivate/Activate form buttons --%>
                <c:if test="${user.id != loggedInUser.id}"> <%-- Prevent self-deactivation --%>
                  <c:if test="${user.isActive == true}">
                    <form action="${pageContext.request.contextPath}/users/manage" method="post" class="action-form">
                      <input type="hidden" name="userId" value="${user.id}">
                      <input type="hidden" name="action" value="deactivate">
                      <button type="submit" class="btn btn-sm btn-outline-danger action-btn" onclick="return confirm('Are you sure you want to deactivate user ${user.username}?');">Deactivate</button>
                    </form>
                  </c:if>
                  <c:if test="${user.isActive == false}">
                    <form action="${pageContext.request.contextPath}/users/manage" method="post" class="action-form">
                      <input type="hidden" name="userId" value="${user.id}">
                      <input type="hidden" name="action" value="activate">
                      <button type="submit" class="btn btn-sm btn-outline-success action-btn" onclick="return confirm('Are you sure you want to activate user ${user.username}?');">Activate</button>
                    </form>
                  </c:if>
                </c:if>
                <c:if test="${user.id == loggedInUser.id}">
                  <span class="text-muted">Self</span>
                </c:if>
              </div>
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