<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Product Management</title>
</head>
<body>
    <h1>Product Management</h1> <!-- Moved the heading here -->

    <hr>

    <h2>Products:</h2>
    <ul>
        <!-- Display the list of products here -->
        <c:forEach var="product" items="${product}">
            <li>${product.name} - Price: $${product.price}</li>
        </c:forEach>
    </ul>

    <a href="index.jsp">Go Back</a>
</body>
</html>
