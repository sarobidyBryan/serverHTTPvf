package server;
import log.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Files;
import java.time.Instant;

public  class RequestHandler extends Thread {
   
    public Socket socket;
    String rootDirectory;
    private String currentDirectory = "/";
    private boolean isPhpActive;

    public RequestHandler(Socket socket,String rootDirectory,boolean isPhpActive) {
        this.socket = socket;
        this.rootDirectory = rootDirectory;
        this.isPhpActive = isPhpActive;
    }

    @Override
    public void run() {
        try {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            OutputStream outputStream = socket.getOutputStream();
            String request = bufferedReader.readLine();
            
            if (request != null) {
                String[] requestParts = request.split(" ");
                String method = requestParts[0]; 
                String filePath = requestParts[1]; 
                String[] correctFilepath = filePath.split("\\?"); 
                
                Logger.log(socket.getInetAddress().getHostAddress(), method, filePath, "HTTP/1.1", 200, 0, "-", "-");
                
                if (method.equals("GET")) {
                    getRequestHandler(bufferedReader, filePath, outputStream, correctFilepath);
                } else if (method.equals("POST")) {
                    postRequestHandler(bufferedReader, rootDirectory + filePath, outputStream);
                } else {
                    sendResponse(outputStream, "HTTP/1.1 405 Method Not Allowed", "Method Not Allowed");
                }
            }
    
            bufferedReader.close();
            outputStream.close();
            socket.close();
    
        } catch (IOException e) {
            e.printStackTrace();
            Logger.logInternalError(e.getMessage()); 
        }
    }
    
    public void getRequestHandler(BufferedReader bufferedReader, String filePath, OutputStream outputStream, String[] correctFilepath) throws IOException {
        String requestedPath = correctFilepath[0]; 
        String completePath = rootDirectory + requestedPath; 
        if(completePath.endsWith(".php")){
            if(!isPhpActive){
                String errorPhp = phpNotActivated();
                sendResponse(outputStream, "HTTP/1.1 200 OK", "text/html", errorPhp.getBytes());
                return;
            }
        }
        File requestedFile = new File(completePath);
        if (requestedFile.isDirectory()) {
            currentDirectory = requestedPath.endsWith("/") ? requestedPath : requestedPath + "/";
        }
    
        File file = new File(completePath);
        
        Logger.log(socket.getInetAddress().getHostAddress(), "GET", filePath, "HTTP/1.1", 200, file.length(), "-", "-");
    
        if (file.isDirectory()) {
            File indexPhp = new File(file, "index.php");
            File indexHtml = new File(file, "index.html");
            if (indexPhp.exists()) {
                if(!isPhpActive){
                    String errorPhp = phpNotActivated();
                    sendResponse(outputStream, "HTTP/1.1 200 OK", "text/html", errorPhp.getBytes());
                    return;
                }
                phpHandler(indexPhp.getPath(), outputStream);
                return;
            }
    
            if (indexHtml.exists()) {
                htmlHandler(indexHtml.getPath(), outputStream);
                return;
            }
    
            String listing = generateDirectoryListing(file, currentDirectory);
            sendResponse(outputStream, "HTTP/1.1 200 OK", "text/html", listing.getBytes());
            return;
        }
  
        if (filePath.endsWith(".php")) {
            if(!isPhpActive){
                String errorPhp = phpNotActivated();
                sendResponse(outputStream, "HTTP/1.1 200 OK", "text/html", errorPhp.getBytes());
                return;
            }
            if (correctFilepath.length > 1) {
                String params = correctFilepath[1];
                phpHandler(completePath, params, outputStream, "GET");
            } else {
                phpHandler(completePath, outputStream);
            }
            return;
        }
    
        htmlHandler(completePath, outputStream);
    }
    
    
    public void postRequestHandler(BufferedReader bufferedReader, String filePath, OutputStream outputStream) throws IOException {
        
        
        if(!isPhpActive){
            String errorPhp = phpNotActivated();
            sendResponse(outputStream, "HTTP/1.1 200 OK", "text/html", errorPhp.getBytes());
            return;
        }
        int contentLength = 0;
        String line;
   
        while ((line = bufferedReader.readLine()) != null && !line.isEmpty()) {
            if (line.startsWith("Content-Length:")) {
                contentLength = Integer.parseInt(line.substring("Content-Length:".length()).trim());
            }
        }

        char[] buffer = new char[contentLength];
        String params;
        int bytesRead = 0;
        while (bytesRead < contentLength) {
            int read = bufferedReader.read(buffer, bytesRead, contentLength - bytesRead);
            if (read == -1) {
                break;
            }
            bytesRead += read;
        }
        params = new String(buffer);

        String response = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: text/html\r\n" +
                "Connection: close\r\n\r\n" +
                "POST request processed successfully.";
    
        outputStream.write(response.getBytes());
    
        Logger.log(socket.getInetAddress().getHostAddress(), "POST", filePath, "HTTP/1.1", 200, contentLength, "-", "-");
    
        phpHandler(filePath, params, outputStream, "POST");
    }
    
    public void htmlHandler(String filePath, OutputStream outputStream) throws IOException {
        File file = new File(filePath);
    
        if (file.exists() && file.isFile()) {

            String content = Files.readString(file.toPath());
    
            String baseTag = "<base href=\"" + currentDirectory + "\">";
            content = content.replace("<head>", "<head>" + baseTag);
    
            sendResponse(outputStream, "HTTP/1.1 200 OK", "text/html", content.getBytes());
    
            Logger.log(socket.getInetAddress().getHostAddress(), "GET", filePath, "HTTP/1.1", 200, content.length(), "-", "-");
        } else {
            sendResponse(outputStream, "HTTP/1.1 404 Not Found", "File Not Found");

            Logger.log(socket.getInetAddress().getHostAddress(), "GET", filePath, "HTTP/1.1", 404, 0, "-", "-");
        }
    }
    


    public static void phpHandler(String filePath, String params, OutputStream outputStream, String method) throws IOException {
        try {
            Logger.logEvent("PHP_HANDLER", "Processing PHP file: " + filePath + " with params: " + params);

            String tempFileName = createTempFile(filePath, method);

            ProcessBuilder processBuilder = new ProcessBuilder("/usr/bin/php", tempFileName, params);

            processBuilder.redirectErrorStream(true);
   
            Process process = processBuilder.start();
    
            InputStream scriptOutput = process.getInputStream();
    
            byte[] buffer = new byte[1024];
            int bytesRead;
   
            String responseHeaders = "HTTP/1.1 200 OK\r\n" +
                    "Content-Type: text/html\r\n" +
                    "Connection: close\r\n\r\n";
            outputStream.write(responseHeaders.getBytes());
            Logger.logEvent("PHP_HANDLER", "Sent response headers.");
    
            while ((bytesRead = scriptOutput.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            outputStream.close();
            Logger.logEvent("PHP_HANDLER", "Finished processing PHP file and sent output.");
   
            File tempFile = new File(tempFileName);
            tempFile.delete();
            Logger.logEvent("PHP_HANDLER", "Temporary file " + tempFileName + " deleted.");
        } catch (IOException e) {
            Logger.logEvent("PHP_HANDLER_ERROR", "Error while processing PHP file: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public static void phpHandler(String filePath, OutputStream outputStream) throws IOException {
        try {
            Logger.logEvent("PHP_HANDLER", "Processing PHP file: " + filePath);
    
            // System.out.println("path ato am phphandler " + filePath);
            ProcessBuilder processBuilder = new ProcessBuilder("/usr/bin/php", filePath );
    
            processBuilder.redirectErrorStream(true);
 
            Process process = processBuilder.start();
    
            InputStream scriptOutput = process.getInputStream();
    
            byte[] buffer = new byte[1024];
            int bytesRead;
    
            String responseHeaders = "HTTP/1.1 200 OK\r\n" +
                    "Content-Type: text/html\r\n" +
                    "Connection: close\r\n\r\n";
            outputStream.write(responseHeaders.getBytes());
            Logger.logEvent("PHP_HANDLER", "Sent response headers.");
    
            while ((bytesRead = scriptOutput.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            outputStream.close();
            Logger.logEvent("PHP_HANDLER", "Finished processing PHP file and sent output.");
        } catch (IOException e) {
            Logger.logEvent("PHP_HANDLER_ERROR", "Error while processing PHP file: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public static String createTempFile(String filePath, String method) throws IOException {
        Writer fileWriter = null;
        try {
            Path fileName = Path.of(filePath);
            String str = Files.readString(fileName);
        
            String tempFileName = "./" + Instant.now().toEpochMilli() + ".php";
            fileWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tempFileName), "utf-8"));
 
            str = "<?php parse_str(implode('&', array_slice($argv, 1)), $_"+ method +"); ?> \n\n" + str;
            fileWriter.write(str);
    
            Logger.logEvent("PHP_HANDLER", "Temporary PHP file created: " + tempFileName);
    
            return tempFileName;
        } catch (IOException e) {
            Logger.logEvent("PHP_HANDLER_ERROR", "Error while creating temp PHP file: " + e.getMessage());
            e.printStackTrace();
        }
        finally {
            if (fileWriter != null) {
                fileWriter.close();
            }
        }
    
        return filePath;
    }
    
    public static void sendResponse(OutputStream outputStream, String statusLine, String message) throws IOException {
        String response = statusLine + "\r\n" +
                "Content-Type: text/plain\r\n" +
                "Content-Length: " + message.length() + "\r\n" +
                "Connection: close\r\n\r\n" +
                message;
        outputStream.write(response.getBytes());
        outputStream.flush();
        Logger.logEvent("SEND_RESPONSE", "Sent response: " + statusLine + " " + message);
    }
    

    public static void sendResponse(OutputStream outputStream, String statusLine, String contentType, byte[] content) throws IOException {
        String responseHeaders = statusLine + "\r\n" +
                "Content-Type: " + contentType + "\r\n" +
                "Content-Length: " + content.length + "\r\n" +
                "Connection: close\r\n\r\n";
        outputStream.write(responseHeaders.getBytes());
        outputStream.write(content);
        outputStream.flush();
        Logger.logEvent("SEND_RESPONSE", "Sent response: " + statusLine);
    }
    

    private String generateDirectoryListing(File directory, String requestPath) {
        StringBuilder html = new StringBuilder("<html><body><h1>Index of " + requestPath + "</h1><ul>");
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                String name = file.getName();
                String link = requestPath + name;
                html.append("<li><a href=\"").append(link).append("\">")
                    .append(name)
                    .append(file.isDirectory() ? "/" : "")
                    .append("</a></li>");
            }
        }
        html.append("</ul></body></html>");
        Logger.logEvent("LISTING", "listing for path: " + requestPath);
        return html.toString();
    }

    private String phpNotActivated(){
        System.out.println("phppppp");
        StringBuilder html = new StringBuilder("<html><body><h1>Php is not activated </h1><p>Please activate php in your configuration</p></body></html>");
        Logger.logEvent("ERROR", "Php not activated error");
        return html.toString();
    }


}