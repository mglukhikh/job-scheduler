package ru.digiteklabs.scheduler.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A very simple web server
 *
 * @author Mikhail Glukhikh
 */
public class ToyServer {

    private class ConnectionHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange httpExchange) throws IOException {
            String response = "Hello from a toy server\n" + environment;
            httpExchange.sendResponseHeaders(200, response.length());
            OutputStream out = httpExchange.getResponseBody();
            out.write(response.getBytes());
            out.close();
        }
    }

    private final ExecutorService executor;

    private final HttpServer server;

    private final ToyEnvironment environment = new ToyEnvironment();

    public ToyServer(final int threadNumber) {
        executor = Executors.newFixedThreadPool(threadNumber);
        HttpServer server;
        try {
            server = HttpServer.create(new InetSocketAddress(8080), 0);
            server.createContext("/", new ConnectionHandler());
            server.setExecutor(executor);
        } catch (IOException ex) {
            System.out.println("Failed to create server socket: " + ex.getMessage());
            server = null;
        }
        this.server = server;
        if (this.server != null)
            this.server.start();
    }

    static public void main(String[] args) {
        new ToyServer(1);
    }
}
