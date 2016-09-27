package MitterServer;

import com.google.gson.Gson;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.stream.Collectors;

import Util.Notification;
import Util.NotificationStore;
import Util.RetrievalRequest;
import Util.RetrievalResult;
import Util.SubscriptionRequest;

class MitterServer {
  /**
   * Use an existing HttpServer
   * @param httpServer the server to use
   */
  public MitterServer(HttpServer httpServer) {
    this.server = httpServer;
  }

  /**
   * Creates a HttpServer bound to the InetSocketAddress with the hostname and port specified.
   * @param hostname the hostname for the server
   * @param port the port for the server
   */
  public MitterServer(String hostname, int port) throws IOException {
    // The HttpServer constructor takes an address, and a maximum backlog. If this is < 0, the
    // default is used.
    this(HttpServer.create(new InetSocketAddress(hostname, port), -1));
  }

  public void init() {
    server.createContext("retrieve", new RetrieveHandler());
    server.createContext("subscribe", new SubscribeHandler());
    server.createContext("send", new SendHandler());
  }

  private String getString(InputStream stream) {
    return new BufferedReader(new InputStreamReader(stream)).lines()
                                                            .collect(Collectors.joining("\n"));
  }

  private class SendHandler implements HttpHandler{
    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
      InputStream requestBody = httpExchange.getRequestBody();
      String jsonString = getString(requestBody);
      Notification newNotification = gson.fromJson(jsonString, Notification.class);
      notificationStore.add(newNotification);

      httpExchange.sendResponseHeaders(201, -1);
    }
  }

  private class SubscribeHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
      InputStream requestBody = httpExchange.getRequestBody();
      String requestJson = getString(requestBody);
      SubscriptionRequest request = gson.fromJson(requestJson, SubscriptionRequest.class);
      requestBody.close();

      clientStore.addAll(request.subscriber, request.subscriptions);
      httpExchange.sendResponseHeaders(200, -1);
    }
  }

  private class RetrieveHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
      InputStream requestBody = httpExchange.getRequestBody();
      String requestJson = getString(requestBody);
      RetrievalRequest request = gson.fromJson(requestJson, RetrievalRequest.class);
      requestBody.close();

      RetrievalResult result = new RetrievalResult();
      result.notifications =
        notificationStore.get(request.severity, clientStore.getSubscriptions(request.requesterID));

      httpExchange.sendResponseHeaders(200, 0);

      String resultJson = gson.toJson(result);
      OutputStream responseBody = httpExchange.getResponseBody();
      responseBody.write(resultJson.getBytes());
      responseBody.close();
    }
  }

  private Gson gson = new Gson();
  private HttpServer server;
  private NotificationStore notificationStore;
  private ClientStore clientStore;
}
