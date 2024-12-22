package server;

import log.*;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class HttpServer {

    private final int port;
    private final String rootDirectory;
    private ServerSocket serverSocket;
    private ExecutorService threadPool;
    private final AtomicBoolean running;
    private String php;

    public HttpServer() {
        Configuration conf = new Configuration();
        int port = conf.getPort();
        String rootDirectory = conf.getRootDirectory();
        String php = conf.getPhp();

        this.port = port;
        this.rootDirectory = rootDirectory;
        this.running = new AtomicBoolean(false);
        this.threadPool = Executors.newCachedThreadPool();  
        this.php = php;
    }

    public void start() {
        if (!running.get()) {
            try {
                boolean isPhpActive = true;
                if(php.equals("false")){
                    isPhpActive = false;
                }
                System.out.println("php status: " + isPhpActive);
                serverSocket = new ServerSocket(port);
                running.set(true); 

                Logger.logStartup();

                System.out.println("Server Listening on port " + port);

                threadPool = Executors.newCachedThreadPool();

                while (running.get()) {
                    try {
                        Socket clientSocket = serverSocket.accept();

                        threadPool.execute(new RequestHandler(clientSocket, rootDirectory,isPhpActive));
                    } catch (IOException e) {
                        if (running.get()) {
                            Logger.logInternalError("Error accepting client connection: " + e.getMessage());
                            e.printStackTrace();
                        }
                    }
                }
            } catch (IOException e) {
                Logger.logInternalError("Server failed to start: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println("Server is already running.");
        }
    }

    public void stop() {
        try {
            running.set(false);
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close(); 
            }
            threadPool.shutdownNow(); 
            System.out.println("Server stopped.");

            Logger.logShutdown();
        } catch (IOException e) {
            Logger.logInternalError("Error during server shutdown: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
