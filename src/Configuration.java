package server;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class Configuration {
    public int port;
    public String rootDirectory;
    public String php;
    private final String confPath ;

    public Configuration() {
        confPath = System.getProperty("user.home")+"/myhttpserver/conf/serverhttp.conf";
        try (BufferedReader reader = new BufferedReader(new FileReader(confPath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("=", 2);
                if (parts.length == 2) {
                    String key = parts[0].trim();
                    String value = parts[1].trim();

                    switch (key) {
                        case "port":
                            this.port = Integer.parseInt(value);
                            break;
                        case "rootDirectory":
                            this.rootDirectory = value;
                            break;
                        case "php":
                            this.php = value;
                            System.out.println(this.php);
                            break;
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading configuration file: " + e.getMessage());
        }
    }

    public int getPort() {
        return port;
    }

    public String getRootDirectory() {
        return rootDirectory;
    }

    public String getPhp() {
        return php;
    }
}
