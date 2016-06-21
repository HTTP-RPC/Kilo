<html>
<head>
<title>Test Form</title>
</head>
<body>
<form action="${pageContext.request.contextPath}/test/sum" method="get">
<table>
<tr>
<td>A</td><td><input type="text" name="a" value="2" size="4"/></td>
<td>B</td><td><input type="text" name="b" value="4" size="4"/></td>
</tr>
<tr>
<td colspan="2"><input type="submit" value="Get Sum"/></td>
</tr>
</table>
</form>

<hr/>

<a href="${pageContext.request.contextPath}/test/sumAll?values=1&values=2&values=3&values=4">Sum All</a>

<hr/>

<a href="${pageContext.request.contextPath}/test/inverse?value=true">Inverse</a>

<hr/>

<form action="${pageContext.request.contextPath}/test/characters" method="get">
<table>
<tr>
<td>Text</td><td><input type="text" name="text" value="Hello, World!"/></td>
</tr>
<tr>
<td colspan="2"><input type="submit" value="Get Characters"/></td>
</tr>
</table>
</form>

<hr/>

<form action="${pageContext.request.contextPath}/test/selection" method="post">
<table>
<tr>
<td style="vertical-align:top">Items</td>
<td>
<select multiple name="items">
<option value="one">One</option>
<option value="two">Two</option>
<option value="three">Three</option>
<option value="four">Four</option>
</select>
</td>
</tr>
<tr>
<td colspan="2"><input type="submit" value="Get Selection"/></td>
</tr>
</table>
</form>

<hr/>

<a href="${pageContext.request.contextPath}/test/tree">Tree</a>

<hr/>

<form action="${pageContext.request.contextPath}/test/statistics" method="post">
<table> 
<tr>
<td>Value 1</td><td><input type="text" name="values" value="1" size="4"/></td>
<td>Value 2</td><td><input type="text" name="values" value="3" size="4"/></td>
<td>Value 3</td><td><input type="text" name="values" value="5" size="4"/></td>
</tr>
<tr>
<td colspan="2"><input type="submit" value="Get Statistics"/></td>
</tr>
</table>
</form>

<hr/>

<a href="${pageContext.request.contextPath}/test/testData">Test Data</a>

<hr/>

<a href="${pageContext.request.contextPath}/test/void">Void</a>

<hr/>

<a href="${pageContext.request.contextPath}/test/null">Null</a>

<hr/>

<a href="${pageContext.request.contextPath}/test/localeCode">Locale Code</a>

<hr/>

<a href="${pageContext.request.contextPath}/test/userName">User Name</a>

<hr/>

<form action="${pageContext.request.contextPath}/test/userRoleStatus" method="get">
<table>
<tr>
<td>Role</td><td><input type="text" name="role" value="tomcat"/></td>
</tr>
<tr>
<td colspan="2"><input type="submit" value="Is User in Role"/></td>
</tr>
</table>
</form>

</body>
</html>
