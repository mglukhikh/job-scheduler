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

        static private final String NEW_JOB_FORM = "<form id=\"new\"><table>" +
                "<tr><th></th><th>Name</th><th>Type</th><th>Time</th><th>Parameter</th></tr>" +
                "<tr><td><button type=\"submit\" value=\"new\">New Job</button></td>" +
                "<td><input type=\"text\" name=\"name\"></td>" +
                "<td><select name=\"type\">" +
                "<option value=\"oneshot\">One Shot</option>" +
                "<option value=\"periodic\">Periodic</option>" +
                "<option value=\"sequential\">Sequential</option>" +
                "</select></td>" +
                "<td><input type=\"number\" name=\"time\"></td>" +
                "<td><input type=\"number\" name=\"param\"></td></tr>" +
                "</table></form>";

        @Override
        public void handle(HttpExchange httpExchange) throws IOException {
            System.out.println(httpExchange.getRequestMethod());
            System.out.println(httpExchange.getRequestURI());
            String response = "<p>Hello from a toy server</p>\n" +
                    environment.toHtml() + NEW_JOB_FORM;
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
