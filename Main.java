import com.sun.net.httpserver.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Path;


public class Main{
    public static int port = 8000;
    public static void main(String[] args){
        try{
            Path root = Path.of("public");
            HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/", new RouterHandler());
            server.createContext("/api", new ApiHandler());
            server.setExecutor(null);
            server.start();
            System.out.println("Server started at http://localhost:"+port);
        } catch (IOException e){
            System.err.println("We've ran into an error: " + e);
        }
    }
    static class RouterHandler implements HttpHandler{
       @Override
        public void handle(HttpExchange exchange) throws IOException{
            String path = exchange.getRequestURI().getPath();
            if(path.endsWith("/") && path.length() > 1){
                path = path.substring(0, path.length() - 1);
            }
            if(path.equals("/")){
                serveResource(exchange, "index.html");
                return;
            }
            switch(path){
                case "/login":
                    serveResource(exchange, ("login.html"));
                    break;
                case "/register":
                    serveResource(exchange, ("register.html"));
                    break;
                case "/dashboard":
                    if(!isAuthenticated(exchange)){
                        redirect(exchange, "/login");
                        break;
                    }
                    serveResource(exchange, "dashboard.html");
                    break;
                default:
                    redirect(exchange, "/");
            }

        }
    }

    static class ApiHandler implements HttpHandler{
        @Override
        public void handle(HttpExchange exchange) throws IOException{
            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();

            System.out.println("API Request: " + method + " " + path);

            switch (path){
                case "/api/test":
                    if(method.equals("GET")){
                        sendString(exchange, "{\"username\"}: \"text\", \"role\": \"test\"}", "application/json");
                    } else if (method.equals("POST")){
                        sendString(exchange, "{\"status\": \"test user (not) created\"}", "application/json");
                    } else{
                        sendString(exchange, "{\"error\": \"Method not allowed\"}", "application/json", 405);
                    }
                    break;
                case "/api/test2":
                    if(method.equals("GET")){
                        sendString(exchange, "{\"username\"}: \"text\", \"role\": \"test\"}", "application/json");
                    } else{
                        sendString(exchange, "{\"error\": \"Method not allowed\"}", "application/json", 405);
                    }
                default:
                    sendString(exchange, "{\"error\": \"Not found\"}", "application/json", 404);
            }
        }
    }

    // IMPLEMENT ACTUAL AUTH HERE
    private static boolean isAuthenticated(HttpExchange exchange){
        String cookie = exchange.getRequestHeaders().getFirst("Cookie");
        return cookie != null && cookie.contains("auth=true");
    }

    private static void serveResource(HttpExchange exchange, String resourcePath) throws IOException{
        try(InputStream is = Main.class.getResourceAsStream("/public/" + resourcePath)){
            if(is == null){
                sendString(exchange, "404 - Not Found.", "text/plain", 404);
                return;
            }
            byte[] data = is.readAllBytes();
            String mime = guessMimeType(resourcePath);
            exchange.getResponseHeaders().set("Content-Type", mime);
            exchange.sendResponseHeaders(200, data.length);
            try(OutputStream os = exchange.getResponseBody()){
                os.write(data);
            }
        }
    }
    private static String guessMimeType(String path){
        if (path.endsWith(".html")) return "text/html";
        if (path.endsWith(".css")) return "text/css";
        if (path.endsWith(".js")) return "application/javascript";
        if (path.endsWith(".json")) return "application/json";
        return "application/octet-stream";
    }

    private static void redirect(HttpExchange exchange, String location) throws IOException{
        exchange.getResponseHeaders().set("Location", location);
        exchange.sendResponseHeaders(302, -1);
        exchange.close();
    }

    private static void sendString(HttpExchange exchange, String body, String type) throws IOException{
        sendString(exchange, body, type, 200);
    }

    private static void sendString(HttpExchange exchange, String body, String type, int status) throws IOException{
        byte[] data = body.getBytes();
        exchange.getResponseHeaders().set("Content-Type", type);
        exchange.sendResponseHeaders(status, data.length);
        try (OutputStream os = exchange.getResponseBody()){
            os.write(data);
        }
    }
}