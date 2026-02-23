package tn.finhub.util;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.function.Consumer;

/**
 * Serves a small local HTML page on http://localhost:* that hosts the reCAPTCHA widget.
 * This avoids "about:blank" origin issues when using WebView.loadContent().
 */
public class RecaptchaLocalServer {

    private static RecaptchaLocalServer instance;

    private HttpServer server;
    private volatile String latestToken;
    private volatile Consumer<String> tokenConsumer;
    private volatile String siteKey;
    private volatile int port;

    private RecaptchaLocalServer() {
    }

    public static synchronized RecaptchaLocalServer getInstance() {
        if (instance == null) {
            instance = new RecaptchaLocalServer();
        }
        return instance;
    }

    public synchronized String start(String siteKey, Consumer<String> onToken) {
        this.siteKey = siteKey;
        this.tokenConsumer = onToken;

        if (server != null) {
            return getRecaptchaUrl();
        }

        try {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            port = server.getAddress().getPort();

            server.createContext("/", new RedirectHandler());
            server.createContext("/recaptcha", new RecaptchaPageHandler());
            server.createContext("/token", new TokenHandler());

            server.setExecutor(daemonExecutor("recaptcha-local-server"));
            server.start();

            return getRecaptchaUrl();
        } catch (IOException e) {
            e.printStackTrace();
            server = null;
            port = 0;
            return null;
        }
    }

    public String getRecaptchaUrl() {
        if (port <= 0) {
            return null;
        }
        // Use "localhost" hostname so it matches typical reCAPTCHA domain allowlists.
        return "http://localhost:" + port + "/recaptcha";
    }

    public String getLatestToken() {
        return latestToken;
    }

    private static Executor daemonExecutor(String threadNamePrefix) {
        ThreadFactory tf = r -> {
            Thread t = new Thread(r);
            t.setName(threadNamePrefix + "-" + t.getId());
            t.setDaemon(true);
            return t;
        };
        return Executors.newCachedThreadPool(tf);
    }

    private class RedirectHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                exchange.close();
                return;
            }
            Headers h = exchange.getResponseHeaders();
            h.set("Location", "/recaptcha");
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        }
    }

    private class RecaptchaPageHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                exchange.close();
                return;
            }

            String key = Objects.toString(siteKey, "");
            String html = """
                    <!DOCTYPE html>
                    <html>
                    <head>
                      <meta charset="UTF-8" />
                      <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                      <script src="https://www.google.com/recaptcha/api.js" async defer></script>
                      <script type="text/javascript">
                        function onRecaptchaSuccess(token) {
                          try {
                            if (window.javaBridge && typeof window.javaBridge.onTokenReceived === 'function') {
                              window.javaBridge.onTokenReceived(token);
                            }
                          } catch (e) {}
                          fetch('/token', {
                            method: 'POST',
                            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                            body: 'token=' + encodeURIComponent(token)
                          }).catch(function(){});
                        }

                        function onRecaptchaExpired() {
                          try {
                            if (window.javaBridge && typeof window.javaBridge.onTokenReceived === 'function') {
                              window.javaBridge.onTokenReceived('');
                            }
                          } catch (e) {}
                          fetch('/token', {
                            method: 'POST',
                            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                            body: 'token='
                          }).catch(function(){});
                        }

                        // Detect when reCAPTCHA image challenge opens: 2+ iframes = challenge visible.
                        var lastChallengeOpen = false;
                        function notifyChallengeVisibility() {
                          var iframes = document.querySelectorAll('iframe');
                          var challengeOpen = iframes.length >= 2;
                          if (challengeOpen === lastChallengeOpen) return;
                          lastChallengeOpen = challengeOpen;
                          try {
                            if (window.javaBridge && typeof window.javaBridge.onChallengeVisibilityChanged === 'function') {
                              window.javaBridge.onChallengeVisibilityChanged(challengeOpen);
                            }
                          } catch(e) {}
                        }

                        window.addEventListener('load', function() {
                          var observer = new MutationObserver(function() { notifyChallengeVisibility(); });
                          observer.observe(document.body, { childList: true, subtree: true, attributes: true });
                          notifyChallengeVisibility();
                          // Poll every 300ms so we catch the challenge even if DOM structure varies
                          setInterval(notifyChallengeVisibility, 300);
                        });
                      </script>
                      <style>
                        body {
                          margin: 0;
                          padding: 8px 0 0 0;
                          background: #1E1B2E; /* match app card background to avoid white box */
                          color: #E5E7EB;
                          font-family: Segoe UI, sans-serif;
                        }
                        .label {
                          color:#9CA3AF;
                          font-size:11px;
                          margin-bottom:4px;
                        }
                      </style>
                    </head>
                    <body>
                      <div class="label">Vérification de sécurité</div>
                      <div class="g-recaptcha"
                           data-sitekey="%s"
                           data-callback="onRecaptchaSuccess"
                           data-expired-callback="onRecaptchaExpired"
                           data-theme="dark">
                      </div>
                    </body>
                    </html>
                    """.formatted(key);

            byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
            Headers headers = exchange.getResponseHeaders();
            headers.set("Content-Type", "text/html; charset=UTF-8");
            headers.set("Cache-Control", "no-store");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    private class TokenHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                exchange.close();
                return;
            }

            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            String token = extractFormField(body, "token");
            if (token != null && !token.isBlank()) {
                latestToken = token;
                Consumer<String> consumer = tokenConsumer;
                if (consumer != null) {
                    consumer.accept(token);
                }
            } else {
                // Token expired or reset — clear it
                latestToken = null;
                Consumer<String> consumer = tokenConsumer;
                if (consumer != null) {
                    consumer.accept("");
                }
            }

            byte[] resp = "ok".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
            exchange.sendResponseHeaders(200, resp.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(resp);
            }
        }

        private String extractFormField(String body, String key) {
            if (body == null || body.isBlank() || key == null || key.isBlank()) {
                return null;
            }
            String[] parts = body.split("&");
            for (String p : parts) {
                int idx = p.indexOf('=');
                if (idx <= 0) {
                    continue;
                }
                String k = p.substring(0, idx);
                if (!key.equals(k)) {
                    continue;
                }
                String v = p.substring(idx + 1);
                try {
                    return URLDecoder.decode(v, StandardCharsets.UTF_8);
                } catch (Exception e) {
                    return v;
                }
            }
            return null;
        }
    }
}