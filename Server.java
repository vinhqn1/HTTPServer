import java.net.*;
import java.util.*;
import java.io.*;
import java.nio.file.*;

public class Server {
    public static void main(String... args) throws IOException {
        ServerSocket server = new ServerSocket(80);
        Socket socket = null;
        while ((socket = server.accept()) != null) {
            final Socket threadSocket = socket;
            new Thread(() -> handleRequest(threadSocket)).start();
        }
        server.close();
    }

    public static void handleRequest(Socket socket) {
        try {
            // Read request
            BufferedReader requestReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String requestLine = requestReader.readLine();
            String[] requestThings = requestLine.split(" ");
            String method = requestThings[0];
            String uri = requestThings[1];
            String version = requestThings[2];
            System.out.println(requestLine);

            // Map headers
            HashMap headers = new HashMap<String, String>();
            String tempHeader;
            while ((tempHeader = requestReader.readLine()) != null && !tempHeader.isEmpty()) {
                String[] header = tempHeader.split(": ");
                headers.put(header[0], header[1]);
            }
            System.out.println(headers.toString());
            // read body
            String body = "";
            if (headers.containsKey("Content-Length")) {
                StringBuilder bodyBuilder = new StringBuilder();
                while (requestReader.ready()) {
                    bodyBuilder.append((char)requestReader.read());
                }
                body = bodyBuilder.toString();
            }

            System.out.println(method);
            HttpResponse response;
            if (method.equals("GET")) {
                response = getResponse(uri, headers, body);
            } else if (method.equals("POST")) {
                response = postResponse(uri, headers, body);
            } else if (method.equals("PUT")) {
                response = putResponse(uri, headers, body);
            } else if (method.equals("DELETE")) {
                response = deleteResponse(uri, headers, body);
            } else if (method.equals("OPTIONS")) {
                response = optionsResponse(uri, headers, body);
            } else {
                response = new HttpResponse(400);
            }
            String responseString = response.toString();
            System.out.println(responseString);
            OutputStream outputStream = socket.getOutputStream();
            PrintWriter printWriter = new PrintWriter(outputStream, true);
            printWriter.println(responseString);
            socket.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static HttpResponse getResponse(String uri, HashMap<String, String> headers, String body){
        File file = new File(uri);
        if (file.exists() && file.isFile()) {
            try {
                byte[] content = Files.readAllBytes(file.toPath());
                HashMap responseHeaders = new HashMap<String, String>();
                // responseHeaders.put("Content-Length", Integer.toString(body.length));
                return new HttpResponse(200);
            } catch (IOException e) {
                return new HttpResponse(500);
            }
        } else {
            return new HttpResponse(404);
        }
    }

    private static HttpResponse postResponse(String uri, HashMap<String, String> headers, String body) {
        if (headers.containsKey("Content-Type") &&
           headers.get("Content-Type").equals("text/plain")) {
            File file = new File(uri);
            try {
                FileWriter writer = new FileWriter(file, true);
                writer.write(body);
                writer.close();
                return new HttpResponse(200);
            } catch (IOException e) {
                return new HttpResponse(500);
            }
        } else {
            return new HttpResponse(415);
        }
    }

    private static HttpResponse putResponse(String uri, HashMap<String, String> headers, String body) {
        File file = new File(uri);
        return null;
    }

    private static HttpResponse deleteResponse(String uri, HashMap<String, String> headers, String body){
        Path path = Path.of(uri);
        System.out.println(Files.exists(path));
        if (Files.exists(path)) {
            try {
                Files.delete(path);
                return new HttpResponse(200);
            } catch (Exception ex) {
                return new HttpResponse(500);
            }
        } else {
            return new HttpResponse(404);
        }
    }

    private static HttpResponse optionsResponse(String uri, HashMap<String, String> headers, String body) {
        return null;
    }

    private static class HttpResponse {
        private static int statusCode;
        private static HashMap<String, String> headers;
        private static String body;
        private static String message;
        private HashMap<Integer, String> responseMessages;

        public HttpResponse (int statusCode, HashMap<String, String> headers, String body) {
            this.statusCode = statusCode;
            this.headers = headers;
            this.body = body;
            this.responseMessages = new HashMap<Integer, String>();
            headers.put("Date", new Date().toString());
            responseMessages.put(200, "OK");
            responseMessages.put(201, "Created");
            responseMessages.put(400, "Bad Request");
            responseMessages.put(404, "Not Found");
            responseMessages.put(500, "Internal Server Error");
            this.message = responseMessages.get(statusCode);
        }

        public HttpResponse(int statusCode) {
            this.statusCode = statusCode;
            this.responseMessages = new HashMap<Integer, String>();
            responseMessages.put(200, "OK");
            responseMessages.put(201, "Created");
            responseMessages.put(400, "Bad Request");
            responseMessages.put(404, "Not Found");
            responseMessages.put(500, "Internal Server Error");
            this.message = responseMessages.get(statusCode);
            this.headers = new HashMap<String, String>();
            headers.put("Date", new Date().toString());
        }

        public String toString() {
            StringBuilder response = new StringBuilder();
            response.append("HTTP/1.1 " + statusCode + " " + message + "\r\n");
            for (HashMap.Entry<String, String> header : headers.entrySet()) {
                response.append(header.getKey() + ": " + header.getValue() + "\r\n");
            }
            response.append("\r\n");
            response.append(body);
            return response.toString();
        }
    }
}