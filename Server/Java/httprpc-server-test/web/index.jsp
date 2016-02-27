<html>
<head>
<title>Test Form</title>
</head>
<body>
<form action="${pageContext.request.contextPath}/test/add" method="get">
<table>
<tr>
<td>A</td><td><input type="text" name="a" value="2" size="4"/></td>
<td>B</td><td><input type="text" name="b" value="4" size="4"/></td>
</tr>
<tr>
<td colspan="2"><input type="submit" value="Add"/></td>
</tr>
</table>
</form>

<hr/>

<a href="${pageContext.request.contextPath}/test/addValues?values=1&values=2&values=3&values=4">Add Values</a>

<hr/>

<a href="${pageContext.request.contextPath}/test/invertValue?value=true">Invert Value</a>

<hr/>

<form action="${pageContext.request.contextPath}/test/getCharacters" method="get">
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

<form action="${pageContext.request.contextPath}/test/getSelection" method="post">
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

<form action="${pageContext.request.contextPath}/test/getStatistics" method="post">
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

<a href="${pageContext.request.contextPath}/test/statistics.html?values=1&values=3&values=5">statistics.html</a>

<hr/>

<p>
<a href="${pageContext.request.contextPath}/test/getTestData">Get Test Data</a>
</p>

<p>
<a href="${pageContext.request.contextPath}/test/testdata.html">testdata.html</a><br>
<a href="${pageContext.request.contextPath}/test/testdata.csv">testdata.csv</a><br>
</p>

<hr/>

<a href="${pageContext.request.contextPath}/test/getVoid">Get Void</a>

<hr/>

<a href="${pageContext.request.contextPath}/test/getNull">Get Null</a>

<hr/>

<a href="${pageContext.request.contextPath}/test/getLocaleCode">Get Locale Code</a>

<hr/>

<a href="${pageContext.request.contextPath}/test/getUserName">Get User Name</a>

<hr/>

<form action="${pageContext.request.contextPath}/test/isUserInRole" method="get">
<table>
<tr>
<td>Role</td><td><input type="text" name="role" value="tomcat"/></td>
</tr>
<tr>
<td colspan="2"><input type="submit" value="Is User in Role"/></td>
</tr>
</table>
</form>

<hr/>

<form action="${pageContext.request.contextPath}/test/getAttachmentInfo" method="post" enctype="multipart/form-data">
<table>
<tr>
<td>File 1</td><td><input type="file" name="file1"/></td>
</tr>
<tr>
<td>File 2</td><td><input type="file" name="file2"/></td>
</tr>
<tr>
<td colspan="2"><input type="submit" value="Get Attachment Info"/></td>
</tr>
</table>
</form>

<hr/>

<form action="${pageContext.request.contextPath}/test/getAttachmentInfo" method="post" enctype="multipart/form-data">
<table>
<tr>
<td>File 1</td><td><input type="file" name="files" multiple/></td>
</tr>
<tr>
<td colspan="2"><input type="submit" value="Get Attachment Info"/></td>
</tr>
</table>
</form>

</body>
</html>
