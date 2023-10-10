<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Product Management</title>
</head>
<body>
<center>
    <h1>Welcome to Product Management</h1>

    <form action="product" method="post" enctype="multipart/form-data">
        <label for="name">Product Name:</label>
        <input type="text" id="name" name="name" required><br><br>

        <label for="price">Price:</label>
        <input type="number" id="price" name="price" required><br><br>

        <label for="photo">Product Photo:</label>
        <input type="file" id="photo" name="photo" required><br><br>

        <input type="submit" value="Add Product">
    </form>
</center>
</body>
</html>
