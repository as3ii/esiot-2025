package org.cus.controller;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.cus.model.LocalState;
import org.cus.model.Reading;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Main API handler/controller.
 */
public final class ApiHandler implements AutoCloseable {

    private static final String CLASS_NAME = ApiHandler.class.getName();
    private static final Logger LOGGER = SimpleLogger.getLogger(CLASS_NAME);
    private static final Gson GSON = new Gson();
    private static final String GET = "GET";
    private static final int HTTP_OK = 200;
    private static final int HTTP_BAD_REQUEST = 400;
    private static final int HTTP_FORBIDDEN = 403;
    private static final int HTTP_NOT_FOUND = 404;
    private static final int HTTP_METHOD_NOT_ALLOWED = 405;
    private static final String ERROR = "error";
    private static final int CORE_POOL_SIZE = 8;
    private static final int MAX_POOL_SIZE = 64;
    private static final int THREAD_KEEP_ALIVE_S = 60;
    private static final int QUEUE_SIZE = 128;

    private final HttpServer server;
    private final CusController controller;
    private final File staticDir;

    /**
     * HTTP API handler/controller.
     *
     * @param ip IP to bind
     * @param port port to bind
     * @param controller backend controller
     * @param staticDir location of static files to serve
     * @throws IOException if an I/O error occurs while handling requests
     */
    @SuppressFBWarnings(
        value = "EI_EXPOSE_REP",
        justification = "ApiHandler needs write access to controller"
    )
    public ApiHandler(final String ip, final int port, final CusController controller, final File staticDir) throws IOException {
        this.controller = controller;
        this.staticDir = staticDir;
        if (!staticDir.isDirectory()) {
            throw new IOException("Not a directory: " + staticDir.getPath());
        }

        server = HttpServer.create(new InetSocketAddress(ip, port), 0);
        server.setExecutor(new ThreadPoolExecutor(CORE_POOL_SIZE, MAX_POOL_SIZE,
            THREAD_KEEP_ALIVE_S, TimeUnit.SECONDS, new LinkedBlockingQueue<>(QUEUE_SIZE)));

        server.createContext("/api/state", this::handleState);
        server.createContext("/api/history", this::handleHistory);
        server.createContext("/api/mode", this::handleMode);
        server.createContext("/api/opening", this::handleOpening);

        server.createContext("/", this::handleStatic);

        server.start();
        LOGGER.logp(Level.INFO, CLASS_NAME, "ApiHandler",
            "HTTP server started on " + ip + ":" + port);
    }

    private void handleState(final HttpExchange exchange) throws IOException {
        if (!GET.equals(exchange.getRequestMethod())) {
            respondInvalidMethod(exchange);
            return;
        }
        final List<Reading> history = controller.getLevelHistory();
        respond(exchange, HTTP_OK, Map.of(
            "tmsState", controller.getTmsState(),
            "wcsState", controller.getWcsState(),
            "valveOpening", controller.getOpening(),
            "lastMeasurement", history.isEmpty() ? -1f : history.getLast().level()
        ));
    }

    private void handleHistory(final HttpExchange exchange) throws IOException {
        if (!GET.equals(exchange.getRequestMethod())) {
            respondInvalidMethod(exchange);
            return;
        }
        final String query = exchange.getRequestURI().getQuery();
        List<Reading> history = controller.getLevelHistory();
        if (query != null && query.startsWith("limit=")) {
            try {
                final int limit = Integer.parseInt(query.substring(6));
                if (limit < history.size() && limit > 0) {
                    // Get last $limit elements
                    history = history.subList(history.size() - limit, history.size());
                }
            } catch (final NumberFormatException e) {
                respond(exchange, HTTP_BAD_REQUEST, Map.of(ERROR, "Invalid limit"));
                return;
            }
        }
        respond(exchange, HTTP_OK, history);
    }

    private void handleMode(final HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            respondInvalidMethod(exchange);
            return;
        }
        final String body = readBody(exchange);
        try {
            final ModeRequest json = GSON.fromJson(body, ModeRequest.class);
            if (json == null || json.mode == null) {
                respondBadRequest(exchange, "Invalid JSON body");
                return;
            }
            final String modeStr = json.mode.toLowerCase(Locale.ENGLISH);
            if ("automatic".equals(modeStr)) {
                controller.setLocalState(LocalState.AUTOMATIC);
            } else if ("manual".equals(modeStr)) {
                controller.setLocalState(LocalState.MANUAL);
            } else {
                respondBadRequest(exchange, "Invalid mode: " + json.mode);
                return;
            }
            respond(exchange, HTTP_OK, Map.of("status", "ok"));
        } catch (final JsonSyntaxException | IllegalArgumentException e) {
            respondBadRequest(exchange, e);
        }
    }

    private void handleOpening(final HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            respondInvalidMethod(exchange);
            return;
        }
        final String body = readBody(exchange);
        try {
            final OpeningRequest json = GSON.fromJson(body, OpeningRequest.class);
            if (json == null) {
                respondBadRequest(exchange, "Invalid JSON body");
            } else if (json.opening < 0 || json.opening > 100) {
                respondBadRequest(exchange, "Opening must be 0-100");
            } else {
                controller.setOpening(json.opening);
                respond(exchange, HTTP_OK, Map.of("status", "ok"));
            }
        } catch (final JsonSyntaxException | IllegalArgumentException e) {
            respondBadRequest(exchange, e);
        }
    }

    private void handleStatic(final HttpExchange exchange) throws IOException {
        if (!GET.equals(exchange.getRequestMethod())) {
            respondInvalidMethod(exchange);
            return;
        }
        final String requestPath = exchange.getRequestURI().getPath();
        File file = new File(staticDir, requestPath).getCanonicalFile();
        // Ensure the target file is inside staticDir
        if (!file.getPath().startsWith(staticDir.getCanonicalPath())) {
            respond(exchange, HTTP_FORBIDDEN, Map.of(ERROR, "Forbidden"));
            return;
        }
        // Handle directory access
        if (file.isDirectory()) {
            file = new File(file, "index.html");
        }
        // Check file existence and readability
        if (!file.isFile() || !file.canRead()) {
            respond(exchange, HTTP_NOT_FOUND, Map.of(ERROR, "Not found"));
            return;
        }
        // Actual file send
        final int index = file.getName().lastIndexOf('.');
        final String contentType =
                index <= 0 ? "application/octet-stream" : switch (file.getName().substring(index)) {
                    case ".html" -> "text/html; charset=utf-8";
                    case ".css" -> "text/css; charset=utf-8";
                    case ".js" -> "application/javascript; charset=utf-8";
                    case ".json" -> "application/json";
                    case ".png" -> "image/png";
                    case ".jpg", ".jpeg" -> "image/jpeg";
                    case ".ico" -> "image/x-icon";
                    default -> "application/octet-stream";
                };
        exchange.getResponseHeaders().set("Content-Type", contentType);
        final byte[] data = Files.readAllBytes(file.toPath());
        exchange.sendResponseHeaders(HTTP_OK, data.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(data);
        }
    }

    private static void respond(final HttpExchange exchange, final int status, final Object data) throws IOException {
        final byte[] json = GSON.toJson(data).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, json.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(json);
        }
    }

    private static String readBody(final HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static void respondBadRequest(final HttpExchange exchange, final Exception e) throws IOException {
        respond(exchange, HTTP_BAD_REQUEST, Map.of(ERROR, e.getMessage()));
    }

    private static void respondBadRequest(final HttpExchange exchange, final String s) throws IOException {
        respond(exchange, HTTP_BAD_REQUEST, Map.of(ERROR, s));
    }

    private static void respondInvalidMethod(final HttpExchange exchange) throws IOException {
        respond(exchange, HTTP_METHOD_NOT_ALLOWED, Map.of(ERROR, "Method not allowed"));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
        server.stop(0);
    }

    private record ModeRequest(String mode) { }

    private record OpeningRequest(int opening) { }
}
