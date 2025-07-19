<%-- This JSP is ONLY for rendering the PDF. It must be well-formed XHTML. --%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8" /> <%-- FIXED: Added self-closing slash for XHTML compatibility --%>
  <style>
    /* Basic styles needed for the PDF rendering to look professional */
    body { font-family: Helvetica, Arial, sans-serif; font-size: 10pt; }
    h3 { font-size: 16pt; }
    p { font-size: 10pt; }
    hr { border-top: 1px solid #ccc; }
    .data-table { border-collapse: collapse; width: 100%; margin-top: 20px; }
    .data-table th, .data-table td { border: 1px solid #ccc; padding: 8px; text-align: left; font-size: 9pt;}
    .data-table th { background-color: #f2f2f2; font-weight: bold; }
    .text-red { color: #dc3545; font-weight: bold; }
    .text-green { color: #28a745; font-weight: bold; }
  </style>
</head>
<body>
<h3>${pdfTitle}</h3>
<p>User: ${loggedInUser.username}</p>
<p>Generated on: <fmt:formatDate value="<%=new java.util.Date()%>" pattern="yyyy-MM-dd HH:mm:ss" /></p>
<hr /> <%-- FIXED: Added self-closing slash for XHTML compatibility --%>

<table class="data-table">
  <thead>
  <tr>
    <th>Timestamp</th>
    <th>Account</th>
    <th>Type</th>
    <th>Amount</th>
    <th>Status</th>
    <th>Description</th>
  </tr>
  </thead>
  <tbody>
  <c:forEach var="tx" items="${transactionsForPdf}">
    <tr>
      <td>${tx.formattedTimestamp}</td>
      <td>${tx.accountNumber}</td>
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
</body>
</html>