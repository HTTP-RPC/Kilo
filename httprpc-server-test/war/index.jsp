<%@page pageEncoding="UTF-8"%>

<html>
<head>
<title>HTTP-RPC Server</title>
</head>

<body>

<h2>MongoDB</h2>
<a href="${pageContext.request.contextPath}/math/sum?a=2&b=4">Sum</a></br>
<a href="${pageContext.request.contextPath}/math/sum?values=1&values=2&values=3">Sum Values</a></br>
<a href="${pageContext.request.contextPath}/echo?value=héllo">Echo</a></br>
<a href="${pageContext.request.contextPath}/catalog/items">Items</a></br>
<a href="${pageContext.request.contextPath}/catalog/items/1">Item 1</a></br>

<hr/>

<form action="${pageContext.request.contextPath}/upload" method="post" enctype="multipart/form-data">
<table>
<tr>
<td>File</td><td><input type="file" name="file"/></td>
</tr>
<tr>
<td colspan="2"><input type="submit" value="Upload"/></td>
</tr>
</table>
</form>

<hr/>

<form action="${pageContext.request.contextPath}/upload" method="post" enctype="multipart/form-data">
<table>
<tr>
<td>Files</td><td><input type="file" name="files" multiple/></td>
</tr>
<tr>
<td colspan="2"><input type="submit" value="Upload"/></td>
</tr>
</table>
</form>

<hr>

<a href="${pageContext.request.contextPath}/test?string=héllo&strings=a&strings=b&strings=c&number=123&flag=true">GET</a></br>
<a href="${pageContext.request.contextPath}/test/a/123/b/héllo/c/456/d/göodbye">GET (Key List)</a></br>

<hr/>

<form action="${pageContext.request.contextPath}/test" method="post" enctype="application/x-www-form-urlencoded">
<table>
<tr>
<td>String</td><td><input type="text" name="string" value="héllo"/></td>
</tr>
<tr>
<td>Strings</td>
<td>
<select multiple name="strings">
<option value="a" selected>a</option>
<option value="b" selected>b</option>
<option value="c" selected>c</option>
</select>
</td>
</tr>
<tr>
<td>Number</td><td><input type="text" name="number" value="123"/></td>
</tr>
<tr>
<td>Flag</td><td><input type="text" name="flag" value="true"/></td>
</tr>
<tr>
<td colspan="2"><input type="submit"/></td>
</tr>
</table>
</form>

<hr/>

<form action="${pageContext.request.contextPath}/test" method="post" enctype="multipart/form-data">
<table>
<tr>
<td>String</td><td><input type="text" name="string" value="héllo"/></td>
</tr>
<tr>
<td>Strings</td>
<td>
<select multiple name="strings">
<option value="a" selected>a</option>
<option value="b" selected>b</option>
<option value="c" selected>c</option>
</select>
</td>
</tr>
<tr>
<td>Number</td><td><input type="text" name="number" value="123"/></td>
</tr>
<tr>
<td>Flag</td><td><input type="text" name="flag" value="true"/></td>
</tr>
<tr>
<td>Attachments</td><td><input type="file" name="attachments" multiple/></td>
</tr>
<tr>
<td colspan="2"><input type="submit"/></td>
</tr>
</table>
</form>

<h2>MongoDB</h2>
<a href="${pageContext.request.contextPath}/restaurants.csv?zipCode=10462">restaurants.csv</a></br>
<a href="${pageContext.request.contextPath}/restaurants.html?zipCode=10462">restaurants.html</a></br>
<a href="${pageContext.request.contextPath}/restaurants.json?zipCode=10462">restaurants.json</a></br>
<a href="${pageContext.request.contextPath}/restaurants.xml?zipCode=10462">restaurants.xml</a></br>

<h2>MySQL</h2>
<a href="${pageContext.request.contextPath}/pets.csv?owner=Gwen">pets.csv</a></br>
<a href="${pageContext.request.contextPath}/pets.html?owner=Gwen">pets.html</a></br>
<a href="${pageContext.request.contextPath}/pets.json?owner=Gwen">pets.json</a></br>
<a href="${pageContext.request.contextPath}/pets.xml?owner=Gwen">pets.xml</a></br>

</body>
</html>
