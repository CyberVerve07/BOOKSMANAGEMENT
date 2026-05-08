import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class Main {
    private static final List<Book> books = Collections.synchronizedList(new ArrayList<>());

    public static void main(String[] args) throws IOException {
        books.add(new Book("The Creative Mind", "A. Writer", 19.99, 5));
        books.add(new Book("Modern Java", "B. Developer", 29.90, 10));

        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/", Main::handleStaticFiles);
        server.createContext("/api/books", Main::handleBooksApi);
        server.setExecutor(null);
        server.start();

        System.out.println("Full-stack book store running at http://localhost:8080");
    }

    private static void handleStaticFiles(HttpExchange exchange) throws IOException {
        URI requestUri = exchange.getRequestURI();
        String path = requestUri.getPath();
        if (path.equals("/") || path.isEmpty()) {
            path = "/index.html";
        }

        Path filePath = Paths.get("web", path.substring(1)).normalize();
        Path webRoot = Paths.get("web").toAbsolutePath().normalize();
        if (!filePath.toAbsolutePath().startsWith(webRoot) || Files.isDirectory(filePath) || !Files.exists(filePath)) {
            sendText(exchange, "404 Not Found", 404);
            return;
        }

        String contentType = contentType(filePath);
        byte[] bytes = Files.readAllBytes(filePath);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static void handleBooksApi(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        URI uri = exchange.getRequestURI();
        Map<String, String> queryParams = parseQuery(uri.getRawQuery());

        switch (method.toUpperCase()) {
            case "GET" -> sendJson(exchange, booksToJson(), 200);
            case "POST" -> handleCreateBook(exchange);
            case "PUT" -> handleUpdateBook(exchange, queryParams);
            case "DELETE" -> handleDeleteBook(exchange, queryParams);
            default -> sendText(exchange, "Method not allowed", 405);
        }
    }

    private static void handleCreateBook(HttpExchange exchange) throws IOException {
        String body = readRequestBody(exchange);
        Map<String, String> form = parseForm(body);
        if (!form.containsKey("title") || !form.containsKey("author") || !form.containsKey("price") || !form.containsKey("quantity")) {
            sendText(exchange, "Missing book data", 400);
            return;
        }

        try {
            Book book = new Book(
                    form.get("title"),
                    form.get("author"),
                    Double.parseDouble(form.get("price")),
                    Integer.parseInt(form.get("quantity"))
            );
            books.add(book);
            sendJson(exchange, book.toJson(), 201);
        } catch (NumberFormatException e) {
            sendText(exchange, "Price and quantity must be numeric", 400);
        }
    }

    private static void handleUpdateBook(HttpExchange exchange, Map<String, String> queryParams) throws IOException {
        Optional<Book> book = findBook(queryParams);
        if (book.isEmpty()) {
            sendText(exchange, "Book not found", 404);
            return;
        }

        String body = readRequestBody(exchange);
        Map<String, String> form = parseForm(body);
        Book current = book.get();

        if (form.containsKey("title")) {
            current.setTitle(form.get("title"));
        }
        if (form.containsKey("author")) {
            current.setAuthor(form.get("author"));
        }
        if (form.containsKey("price")) {
            try {
                current.setPrice(Double.parseDouble(form.get("price")));
            } catch (NumberFormatException e) {
                sendText(exchange, "Invalid price format", 400);
                return;
            }
        }
        if (form.containsKey("quantity")) {
            try {
                current.setQuantity(Integer.parseInt(form.get("quantity")));
            } catch (NumberFormatException e) {
                sendText(exchange, "Invalid quantity format", 400);
                return;
            }
        }

        sendJson(exchange, current.toJson(), 200);
    }

    private static void handleDeleteBook(HttpExchange exchange, Map<String, String> queryParams) throws IOException {
        Optional<Book> book = findBook(queryParams);
        if (book.isEmpty()) {
            sendText(exchange, "Book not found", 404);
            return;
        }
        books.remove(book.get());
        sendText(exchange, "Book deleted", 200);
    }

    private static Optional<Book> findBook(Map<String, String> queryParams) {
        if (!queryParams.containsKey("id")) {
            return Optional.empty();
        }
        try {
            int id = Integer.parseInt(queryParams.get("id"));
            return books.stream().filter(book -> book.getId() == id).findFirst();
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private static String booksToJson() {
        StringBuilder builder = new StringBuilder();
        builder.append("[");
        synchronized (books) {
            for (int i = 0; i < books.size(); i++) {
                builder.append(books.get(i).toJson());
                if (i < books.size() - 1) {
                    builder.append(",");
                }
            }
        }
        builder.append("]");
        return builder.toString();
    }

    private static Map<String, String> parseQuery(String query) {
        Map<String, String> result = new HashMap<>();
        if (query == null || query.isBlank()) {
            return result;
        }
        for (String param : query.split("&")) {
            String[] parts = param.split("=", 2);
            if (parts.length == 2) {
                result.put(urlDecode(parts[0]), urlDecode(parts[1]));
            }
        }
        return result;
    }

    private static String readRequestBody(HttpExchange exchange) throws IOException {
        InputStream inputStream = exchange.getRequestBody();
        return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    }

    private static Map<String, String> parseForm(String body) {
        Map<String, String> result = new HashMap<>();
        if (body == null || body.isBlank()) {
            return result;
        }
        for (String part : body.split("&")) {
            String[] parts = part.split("=", 2);
            if (parts.length == 2) {
                result.put(urlDecode(parts[0]), urlDecode(parts[1]));
            }
        }
        return result;
    }

    private static String urlDecode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private static void sendJson(HttpExchange exchange, String response, int status) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static void sendText(HttpExchange exchange, String response, int status) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static String contentType(Path filePath) {
        String fileName = filePath.getFileName().toString().toLowerCase();
        if (fileName.endsWith(".css")) {
            return "text/css; charset=UTF-8";
        }
        if (fileName.endsWith(".js")) {
            return "application/javascript; charset=UTF-8";
        }
        return "text/html; charset=UTF-8";
    }
}
