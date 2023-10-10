import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;

@WebServlet("/product")
@MultipartConfig
public class ProductServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private Connection connection;
    private CloudBlobContainer blobContainer;

    public ProductServlet() {
        super();
    }

    @Override
    public void init() throws ServletException {
        try {
            // Load the database configuration from a properties file
            Properties props = new Properties();
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream("db.properties");
            props.load(inputStream);

            // Load the SQL Server JDBC driver
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");

            // Connect to the Azure SQL Database
            String url = props.getProperty("url");
            String username = props.getProperty("username");
            String password = props.getProperty("password");
            connection = DriverManager.getConnection(url, username, password);

            // Connect to the Azure Blob Storage
            String storageConnectionString = props.getProperty("storageConnectionString");
            CloudStorageAccount storageAccount = CloudStorageAccount.parse(storageConnectionString);
            CloudBlobClient blobClient = storageAccount.createCloudBlobClient();
            blobContainer = blobClient.getContainerReference("product-photos");
            blobContainer.createIfNotExists();
        } catch (IOException | SQLException | ClassNotFoundException | URISyntaxException | InvalidKeyException | StorageException e) {
            e.printStackTrace();
        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String productName = request.getParameter("name");
        int price = Integer.parseInt(request.getParameter("price"));
        Part photo = request.getPart("photo");

        try {
            // Insert the product details into the Azure SQL table
            String sql = "INSERT INTO dbo.Product (ProductName, Price) VALUES (?, ?)";
            PreparedStatement statement = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);
            statement.setString(1, productName);
            statement.setInt(2, price);
            int affectedRows = statement.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Creating product failed, no rows affected.");
            }

            // Retrieve the generated ProductId
            ResultSet generatedKeys = statement.getGeneratedKeys();
            int productId = -1;
            if (generatedKeys.next()) {
                productId = generatedKeys.getInt(1);
            } else {
                throw new SQLException("Creating product failed, no ID obtained.");
            }

            // Upload the product photo to Azure Blob Storage
            String photoFileName = photo.getSubmittedFileName();
            CloudBlockBlob blob = blobContainer.getBlockBlobReference(productId + "_" + photoFileName);
            blob.upload(photo.getInputStream(), photo.getSize());
        } catch (SQLException | URISyntaxException | StorageException e) {
            e.printStackTrace();
        }

        response.sendRedirect("product.jsp");
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            // Retrieve product details from the Azure SQL table
            String sql = "SELECT * FROM dbo.Product";
            PreparedStatement statement = connection.prepareStatement(sql);
            ResultSet resultSet = statement.executeQuery();

            // Create a list to hold product objects
            List<Product> productsList = new ArrayList<>();

            while (resultSet.next()) {
                // Create a Product object for each row in the ResultSet
                Product product = new Product(resultSet.getInt("id"), resultSet.getString("ProductName"), resultSet.getInt("Price"));

                // Add the Product object to the list
                productsList.add(product);
            }

            // Store the list of products in the request scope
            request.setAttribute("products", productsList);

            // Forward the request to the JSP for rendering
            request.getRequestDispatcher("product.jsp").forward(request, response);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void destroy() {
        try {
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
