<%@page pageEncoding="UTF-8"%>

<html>
<head>
<title>HTTP-RPC Test</title>
</head>

<body>

<a href="${pageContext.request.contextPath}/math?api">Math API</a><br/>
<br/>
<a href="${pageContext.request.contextPath}/math/sum?a=2&b=4">Sum</a><br/>
<a href="${pageContext.request.contextPath}/math/sum?values=1&values=2&values=3">Sum Values</a><br/>

<hr/>

<a href="${pageContext.request.contextPath}/echo?api">Echo API</a><br/>
<br/>
<a href="${pageContext.request.contextPath}/echo?value=héllo">Echo</a><br/>

<hr/>

<a href="${pageContext.request.contextPath}/catalog?api">Catalog API</a><br/>
<a href="${pageContext.request.contextPath}/upload?api">Upload API</a><br/>
<a href="${pageContext.request.contextPath}/tree?api">Tree API</a><br/>
<a href="${pageContext.request.contextPath}/test?api">Test API</a><br/>

<hr/>

<a href="${pageContext.request.contextPath}/system-info?api">System Info API</a><br/>
<br/>
<a href="${pageContext.request.contextPath}/system-info">System Info</a><br/>

<hr/>

<a href="${pageContext.request.contextPath}/pets?api">Pets API</a><br/>
<br/>
<a href="${pageContext.request.contextPath}/pets?owner=Gwen">Pets (JSON)</a><br/>
<a href="${pageContext.request.contextPath}/pets?owner=Gwen&format=csv">Pets (CSV)</a><br/>
<a href="${pageContext.request.contextPath}/pets?owner=Gwen&format=html">Pets (HTML)</a><br/>
<br/>
<a href="${pageContext.request.contextPath}/pets/average-age">Average Age</a><br/>

<hr/>

<a href="${pageContext.request.contextPath}/employees?api">Employees API</a><br/>

</body>
</html>
