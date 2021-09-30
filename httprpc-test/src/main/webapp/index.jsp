<%@page pageEncoding="UTF-8"%>

<html>
<head>
<title>HTTP-RPC Test</title>
</head>

<body>

<a href="${pageContext.request.contextPath}/test?api">Test API</a><br/>
<a href="${pageContext.request.contextPath}/upload?api">Upload API</a><br/>
<a href="${pageContext.request.contextPath}/math?api">Math API</a><br/>
<a href="${pageContext.request.contextPath}/tree?api">Tree API</a><br/>
<a href="${pageContext.request.contextPath}/catalog?api">Catalog API</a><br/>
<a href="${pageContext.request.contextPath}/employees?api">Employees API</a><br/>

<hr/>

<a href="${pageContext.request.contextPath}/pets?api">Pets API</a><br/>
<br/>
<a href="${pageContext.request.contextPath}/pets?owner=Gwen">Pets (JSON)</a><br/>
<a href="${pageContext.request.contextPath}/pets?owner=Gwen&format=csv">Pets (CSV)</a><br/>
<a href="${pageContext.request.contextPath}/pets?owner=Gwen&format=html">Pets (HTML)</a><br/>
<br/>
<a href="${pageContext.request.contextPath}/pets/average-age">Average Age</a><br/>

</body>
</html>
