package main;

import server.*;
import javax.swing.*;
import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        HttpServer server = new HttpServer();

        server.start(); 

    }
}
