<%@page pageEncoding="UTF-8"%>

<html>
<head>
<title>Pet Service Examples</title>
</head>
<body>
<a href="${pageContext.request.contextPath}/pets?owner=Gwen">Pets (JSON)</a><br/>
<br/>
<a href="${pageContext.request.contextPath}/pets?owner=Gwen&format=csv">Pets (CSV)</a><br/>
<a href="${pageContext.request.contextPath}/pets?owner=Gwen&format=html">Pets (HTML)</a><br/>
<br/>
<a href="${pageContext.request.contextPath}/pets/average-age">Average Age</a><br/>
</body>
</html>
