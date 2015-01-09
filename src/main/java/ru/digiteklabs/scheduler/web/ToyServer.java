package ru.digiteklabs.scheduler.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.jetbrains.annotations.NotNull;
import ru.digiteklabs.scheduler.core.api.SchedulingException;
import ru.digiteklabs.scheduler.job.api.Job;
import ru.digiteklabs.scheduler.job.samples.*;

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

        static private final String HTML_HEAD = "<head><title>Toy Scheduling Server</title>" +
                "<meta http-equiv=\"refresh\" content=\"60\" /></head>";

        static private final String NEW_JOB_FORM = "<form id=\"new\"><table>" +
                "<tr><th></th><th>Name</th><th>Type</th>" +
                "<th>Start time (ms from now)</th><th>Duration (ms)/Limit/Number to check</th></tr>" +
                "<tr><td><button type=\"submit\" value=\"new\">New Job</button></td>" +
                "<td><input type=\"text\" name=\"name\"></td>" +
                "<td><select name=\"type\">" +
                "<option value=\"oneshot\">One Shot</option>" +
                "<option value=\"periodic\">Periodic</option>" +
                "<option value=\"sequential\">Sequential</option>" +
                "<option value=\"calculator\">Prime Calculator</option>" +
                "<option value=\"checker\">Prime Checker</option>" +
                "</select></td>" +
                "<td><input type=\"number\" name=\"time\"></td>" +
                "<td><input type=\"number\" name=\"param\"></td></tr>" +
                "</table></form>";

        static private final String REFRESH_FORM = "<form id=\"refresh\">" +
                "<button type=\"submit\" value=\"doit\">Refresh</button></form>";

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
                if (name == null || name.isEmpty() || type == null)
                    return;
                final String time = parsed.get("time");
                final String param = parsed.get("param");
                if (time == null || param == null)
                    return;
                final Job job = createJob(type, Integer.parseInt(time), Integer.parseInt(param));
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
            final URI uri = httpExchange.getRequestURI();
            final String query = uri.getQuery();
            if (query != null) {
                handleQuery(query);
            }
            String response = "<html>" + HTML_HEAD + "<body><p>Hello from a toy server</p>\n" +
                    environment.toHtml() + NEW_JOB_FORM + REFRESH_FORM +
                    "<p><b>" + status + "</b></p></body></html>";
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

    private Job createJob(final String type, final int time, final int param) {
        final Date date = new Date(Calendar.getInstance().getTimeInMillis() + time);
        if ("oneshot".equals(type)) {
            return new OneShotJob(date, param);
        } else if ("periodic".equals(type)) {
            return new PeriodicJob(date, param, time);
        } else if ("sequential".equals(type)) {
            return new SequentialJob(10, date, param);
        } else if ("calculator".equals(type)) {
            return new PrimeCalcJob(date, param);
        } else if ("checker".equals(type)) {
            final PrimeCalcJob calcJob = environment.getCalcJob();
            if (calcJob == null)
                return null;
            else
                return new PrimeCheckJob(date, calcJob, param);
        } else {
            return null;
        }
    }

    /**
     * Constructs a new toy server with a given thread number
     * @param threadNumber a necessary thread number
     */
    public ToyServer(final int threadNumber) {
        executor = Executors.newFixedThreadPool(threadNumber);
        HttpServer server;
        try {
            server = HttpServer.create(new InetSocketAddress(8080), 0);
            server.createContext("/", new ConnectionHandler());
            server.setExecutor(executor);
            System.out.println("A toy server started successfully");
        } catch (IOException ex) {
            System.out.println("Failed to create server socket: " + ex.getMessage());
            server = null;
        }
        this.server = server;
        if (this.server != null)
            this.server.start();
    }

    /**
     * A classic main function
     * @param args command line arguments, not in use
     */
    static public void main(String[] args) {
        new ToyServer(1);
    }
}
