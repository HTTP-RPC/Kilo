<%@page pageEncoding="UTF-8"%>

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

<a href="${pageContext.request.contextPath}/test/sum?values=1&values=2&values=3&values=4">Sum</a>

<hr/>

<a href="${pageContext.request.contextPath}/test/inverse?value=true">Inverse</a>

<hr/>

<form action="${pageContext.request.contextPath}/test/characters" method="get">
<table>
<tr>
<td>Text</td><td><input type="text" name="text" value="Héllo, World!"/></td>
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
<option value="å">One</option>
<option value="b">Two</option>
<option value="c">Three</option>
<option value="d">Four</option>
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
<br/>
<a href="${pageContext.request.contextPath}/test/tree.html">tree.html</a>

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

<a href="${pageContext.request.contextPath}/test/testData">Test Data</a>
<br/>
<a href="${pageContext.request.contextPath}/test/testData.html">testData.html</a>
<br>
<a href="${pageContext.request.contextPath}/test/testData.xml">testData.xml</a>
<br>
<a href="${pageContext.request.contextPath}/test/testData.csv">testData.csv</a>
<br>

<hr/>

<a href="${pageContext.request.contextPath}/test/void">Void</a>

<hr/>

<a href="${pageContext.request.contextPath}/test/null">Null</a>

<hr/>

<a href="${pageContext.request.contextPath}/test/localeCode">Locale Code</a>

<hr/>

<a href="${pageContext.request.contextPath}/test/user/name">User Name</a>

<hr/>

<form action="${pageContext.request.contextPath}/test/user/roleStatus" method="get">
<table>
<tr>
<td>Role</td><td><input type="text" name="role" value="tomcat"/></td>
</tr>
<tr>
<td colspan="2"><input type="submit" value="Get User Role Status"/></td>
</tr>
</table>
</form>

<hr/>

<form action="${pageContext.request.contextPath}/test/attachmentInfo" method="post" enctype="multipart/form-data" accept-charset="UTF-8">
<table>
<tr>
<td>Text</td><td><input type="text" name="text" value="héllo"/></td>
</tr>
<tr>
<td>File 1</td><td><input type="file" name="attachments"/></td>
</tr>
<tr>
<td>File 2</td><td><input type="file" name="attachments"/></td>
</tr>
<tr>
<td colspan="2"><input type="submit" value="Get Attachment Info"/></td>
</tr>
</table>
</form>

<hr/>

<form action="${pageContext.request.contextPath}/test/attachmentInfo" method="post" enctype="multipart/form-data" accept-charset="UTF-8">
<table>
<tr>
<td>Text</td><td><input type="text" name="text" value="héllo"/></td>
</tr>
<tr>
<td>Files</td><td><input type="file" name="attachments" multiple/></td>
</tr>
<tr>
<td colspan="2"><input type="submit" value="Get Attachment Info"/></td>
</tr>
</table>
</form>

</body>
</html>
