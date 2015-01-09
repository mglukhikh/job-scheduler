package ru.digiteklabs.scheduler.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.jetbrains.annotations.NotNull;
import ru.digiteklabs.scheduler.core.api.SchedulingException;
import ru.digiteklabs.scheduler.job.api.Job;
import ru.digiteklabs.scheduler.job.samples.OneShotJob;
import ru.digiteklabs.scheduler.job.samples.PeriodicJob;
import ru.digiteklabs.scheduler.job.samples.SequentialJob;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.*;
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

        private Map<String, String> parseQuery(final @NotNull String query) {
            final String[] content = query.split("&");
            final Map<String, String> result = new HashMap<String, String>();
            for (String expr: content) {
                final String[] values = expr.split("=");
                if (values.length == 2) {
                    result.put(values[0], values[1]);
                }
            }
            return result;
        }

        private void handleQuery(final @NotNull String query) {
            final Map<String, String> parsed = parseQuery(query);
            try {
                if (parsed.containsKey("remove")) {
                    final String name = parsed.get("remove");
                    if (environment.removeJob(name))
                        status = "OK";
                    else
                        status = "Error removing job " + name;
                    return;
                }
                final String name = parsed.get("name");
                final String type = parsed.get("type");
                if (name == null || name.isEmpty() || type == null || type.isEmpty())
                    return;
                final String time = parsed.get("time");
                final String param = parsed.get("param");
                if (time == null || time.isEmpty() || param == null || param.isEmpty())
                    return;
                final int nTime = Integer.parseInt(time);
                final int nParam = Integer.parseInt(param);
                final Date date = new Date(Calendar.getInstance().getTimeInMillis() + nTime);
                final Job job;
                if ("oneshot".equals(type)) {
                    job = new OneShotJob(date, nParam);
                } else if ("periodic".equals(type)) {
                    job = new PeriodicJob(date, nParam, nTime);
                } else if ("sequential".equals(type)) {
                    job = new SequentialJob(10, date, nParam);
                } else {
                    job = null;
                }
                if (job != null) {
                    if (environment.addJob(name, job))
                        status = "OK";
                    else
                        status = "Error adding job " + name;
                }
            } catch (NumberFormatException ex) {
                // JUST RETURN
            } catch (SchedulingException ex) {
                status = "Error: " + ex.getMessage();
            }
        }

        @Override
        public void handle(HttpExchange httpExchange) throws IOException {
            System.out.println(httpExchange.getRequestMethod());
            final URI uri = httpExchange.getRequestURI();
            final String query = uri.getQuery();
            if (query != null) {
                handleQuery(query);
                System.out.println(query);
            }
            String response = "<p>Hello from a toy server</p>\n" +
                    environment.toHtml() + NEW_JOB_FORM + "<p><b>" + status + "</b></p>";
            httpExchange.sendResponseHeaders(200, response.length());
            OutputStream out = httpExchange.getResponseBody();
            out.write(response.getBytes());
            out.close();
        }
    }

    private final ExecutorService executor;

    private final HttpServer server;

    private final ToyEnvironment environment = new ToyEnvironment();

    private String status = "OK";

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
