<%-- This JSP is ONLY for rendering the PDF. It contains its own styles. --%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8" />
  <style>
    /* These styles will ONLY be applied to the PDF document */
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
<%-- The PDF-specific header text now lives here --%>
<h3>${pdfTitle}</h3>
<p>User: ${loggedInUser.username}</p>
<p>Generated on: <fmt:formatDate value="<%=new java.util.Date()%>" pattern="yyyy-MM-dd HH:mm:ss" /></p>
<hr />

<%-- Include the clean, shared table fragment --%>
<%@ include file="/WEB-INF/jspf/_transactionTable.jspf" %>

</body>
</html>