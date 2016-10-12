package server;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import util.CreatedNotification;
import util.LamportClock;
import util.Notification;
import util.NotificationStore;
import util.Registration;
import util.RetrievalResult;
import util.Severity;
import util.SubscriptionResult;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class MitterServer {
  /**
   * Use an existing HttpServer.
   * @param httpServer the server to us
   */
  public MitterServer(HttpServer httpServer, MulticastSocket multicastSocket, InetAddress
                                                                              broadcastGroup) throws IOException {
    server = httpServer;
    server.setExecutor(Executors.newCachedThreadPool());
    clientStore = new ClientStore();
    notificationStore = new NotificationStore();
    clock = new LamportClock();
    multicast = multicastSocket;
    this.broadcastGroup = broadcastGroup;
    this.multicastPort = multicastSocket.getLocalPort();
    multicast.joinGroup(InetAddress.getByName("224.4.4.4"));
  }

  /**
   * Creates a HttpServer bound to the InetSocketAddress with the hostname and port specified.
   * @param hostname the hostname for the server
   * @param serverPort the port for the server
   */
  public MitterServer(String hostname, int serverPort, int multicastPort) throws IOException {
    // The HttpServer constructor takes an address, and a maximum backlog. If this is < 0, the
    // default is used.

    this(HttpServer.create(new InetSocketAddress(hostname, serverPort), -1),
         new MulticastSocket(multicastPort), InetAddress.getByName("224.4.4.4"));
  }

  /**
   * Initialise the server. Creates the contexts, and initialise HTTP Handlers.
   */
  public void init() {
    server.createContext("/retrieve", new RetrieveHandler());
    server.createContext("/subscribe", new SubscribeHandler());
    server.createContext("/send", new SendHandler());
    server.createContext("/register", new RegisterHandler());
  }

  /**
   * Starts the server. This must be called before requests are received.
   */
  public void start() {
    server.start();
  }

  /**
   * Reads an InputStream into a string
   * @param stream the stream of text to be read
   * @return a string of text, separated by \n characters.
   */
  private String readToString(InputStream stream) {
    return new BufferedReader(new InputStreamReader(stream)).lines()
                                                            .collect(Collectors.joining("\n"));
  }

  private void broadcast(String notification) {
    byte[] message = notification.getBytes();
    DatagramPacket packet = new DatagramPacket(message, message.length, broadcastGroup, multicastPort);
    try {
      multicast.send(packet);
      System.out.println("Broadcast a notification of size " + packet.getLength());
      System.out.println(message);
    } catch (IOException e) {
      System.err.println("Had trouble connecting. Try again.");
    }
  }

  /**
   * Handles requests from notification servers to send notifications to clients.
   *
   * <p>SendHandler expects a POST request. The body of the request should contain a JSON string
   * that can be deserialized into a @link{util.Notification}. The ID parameter is not required.
   * If it is present, the handler will assume the sender wished to update that notification. The
   * GSON library is used for deserialization.
   *
   * <p>It is expected that the @code{logicalTimestamp} in the notification is the sender's current
   * Lamport time, regardless of whether it is a new notification, or an update. This means that
   * the notification server should update the @code{logicalTimestamp} when they do other updates.
   *
   * <p>The handler will respond with a notification, with the ID attached. For new events, this
   * will be newly generated. For updated notifications, the ID remains the same. The sender
   * should store the ID if they may wish to update the notification in the future.
   *
   * <p>Separately from the notification, the server's current Timestamp will be sent.
   */
  private class SendHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
      InputStream requestBody = httpExchange.getRequestBody();
      String jsonString = readToString(requestBody);
      Notification newNotification;
      try {
        newNotification = gson.fromJson(jsonString, Notification.class);
      } catch (JsonSyntaxException jsonSyntax) {
        httpExchange.sendResponseHeaders(400, 0);
        OutputStream responseBody = httpExchange.getResponseBody();
        OutputStreamWriter responseWriter = new OutputStreamWriter(responseBody);
        gson.toJson(jsonSyntax, responseWriter);
        responseWriter.close();
        // We don't wish to continue processing the request once we've alerted the client that
        // it's malformed.
        return;
      }
      System.out.println(newNotification.toString());

      clock.receive(newNotification.logicalTimestamp);
      newNotification.logicalTimestamp = clock.getTime();
      notificationStore.add(newNotification);

      if (newNotification.severity == Severity.URGENT) {
        broadcast(jsonString);
      }

      httpExchange.sendResponseHeaders(201, 0);
      clock.send();
      OutputStream responseBody = httpExchange.getResponseBody();
      OutputStreamWriter responseWriter = new OutputStreamWriter(responseBody);
      gson.toJson(new CreatedNotification(clock.getTime(), newNotification), responseWriter);
      responseWriter.close();
    }
  }

  /**
   * Handles requests from clients to subscribe to notification servers.
   *
   * <p>The SubscribeHandler expects a GET request. It should contain at least three parameters:
   * <ul>
   *   <li>id: the client's ID
   *   <li>subscription: the ID of the notification server the client wishes to subscribe to
   *   <li>time: the time component of the client's Lamport Clock
   * </ul>
   *
   *  <p>To subscribe to multiple notification servers in one request, append more "subscription"
   *  parameters.
   *
   *  <p>The handler will respond with a JSON object containing all of the client's
   *  subscriptions, as well as a @link{util.Timestamp}.
   */
  private class SubscribeHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
      String rawId = "";
      int time = -1;
      ArrayList<String> rawSubscriptions = new ArrayList<>();
      List<NameValuePair> params = URLEncodedUtils.parse(httpExchange.getRequestURI(), "UTF-8");
      for (NameValuePair param : params) {
        if (param.getName().equals("id")) {
          rawId = param.getValue();
        } else if (param.getName().equals("subscription")) {
          rawSubscriptions.add(param.getValue());
        } else if (param.getName().equals("time")) {
          time = Integer.parseInt(param.getValue());
        }
      }

      clock.receive(new util.Timestamp(time));
      UUID subscriber = UUID.fromString(rawId);
      ArrayList<UUID> subscriptions = rawSubscriptions.stream().map(UUID::fromString)
                                        .collect(Collectors.toCollection(ArrayList::new));

      System.out.println("Subscribed " + subscriber + " to " + subscriptions);

      clientStore.addAll(subscriber, subscriptions);

      clock.send();
      httpExchange.sendResponseHeaders(200, 0);
      OutputStream responseBody = httpExchange.getResponseBody();
      OutputStreamWriter responseWriter = new OutputStreamWriter(responseBody);

      gson.toJson(new SubscriptionResult(clientStore.getSubscriptions(subscriber),
                                         clock.getTime()),
                  responseWriter);
      responseWriter.close();
    }
  }

  /**
   * Handles requests from clients for notifications.
   *
   * <p>The handler expects a GET request, with three parameters:
   * <ul>
   *   <li>id: the client's ID
   *   <li>severity: the type of notifications to retrieve. Must be one of the following:
   *   <ul>
   *     <li>"notice"
   *     <li>"caution"
   *     <li>"urgent"
   *   </ul>
   *   <li>time: the time component of the client's Lamport Clock
   * </ul>
   *
   * <p>While the client can retrieve urgent notifications, this is not the ideal method. The
   * client should listen to the urgent broadcast, and perform filtering for their subscriptions.
   *
   * <p>The handler will respond with a JSON object that can be deserialized into a @link{util
   * .RetrievalResult}, which wraps a list of notifications matching the criteria provided. It
   * will also send a @link{util.Timestamp}.
   */
  private class RetrieveHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
      UUID id = null;
      Severity severity = null;
      int time = -1;

      // Parse the above parameters from the request URI. Creates a list of items (name, value).
      List<NameValuePair> params = URLEncodedUtils.parse(httpExchange.getRequestURI(), "UTF-8");
      for (NameValuePair param : params) {
        if (param.getName().equals("id")) {
          id = UUID.fromString(param.getValue());
        } else if (param.getName().equals("severity")) {
          // Unfortunately, there isn't a nice way to instantiate enum values. At the moment,
          // this is the only place this behaviour is required.
          switch (param.getValue()) {
            case "caution":
              severity = Severity.CAUTION;
              break;
            case "notice":
              severity = Severity.NOTICE;
              break;
            case "urgent":
              severity = Severity.URGENT;
              break;
            default:
              break;
          }
        } else if (param.getName().equals("time")) {
          time = Integer.parseInt(param.getValue());
        } else if (param.getName().equals("since")) {
          time = Integer.parseInt(param.getValue());
        }
      }

      clock.receive(new util.Timestamp(time));

      System.out.println("Requested notifications for " + id + ", with severity " + severity);

      RetrievalResult result = new RetrievalResult();
      result.notifications = notificationStore.get(severity,
                                                 clientStore.getSubscriptions(id),
                                                 clientStore.getLastAccess(id));
//                                                 null);

      clock.send();
      clientStore.setLastAccess(id, clock.getTime());
      result.timestamp = clock.getTime();

      httpExchange.sendResponseHeaders(200, 0);
      OutputStream responseBody = httpExchange.getResponseBody();

      OutputStreamWriter responseWriter = new OutputStreamWriter(responseBody);
      gson.toJson(result, responseWriter);

      responseWriter.close();
    }
  }

  /**
   * A handler for registering clients and notification servers.
   *
   * <p>A GET request to this handler returns a universally unique identifier, and a
   * link{util.Timestamp}. The sender must store this number, and use it to identify itself in
   * future requests to the Mitter Server.
   *
   * <p>It is expected that the request has a timestamp associated with it, though only the time
   * component should be sent.
   */
  private class RegisterHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
      List<NameValuePair> params = URLEncodedUtils.parse(httpExchange.getRequestURI(), "UTF-8");
      int time = -1;
      for (NameValuePair param : params) {
        if (param.getName().equals("time")) {
          time = Integer.parseInt(param.getValue());
        }
      }

      clock.receive(new util.Timestamp(time));

      httpExchange.sendResponseHeaders(200, 0);
      OutputStream responseBody = httpExchange.getResponseBody();
      OutputStreamWriter responseWriter = new OutputStreamWriter(responseBody);

      clock.send();
      Registration registration = new Registration(clock.getTime(), UUID.randomUUID());
      gson.toJson(registration, responseWriter);

      responseWriter.close();
    }
  }

  MulticastSocket multicast;
  private InetAddress broadcastGroup;
  private int multicastPort;

  private LamportClock clock;
  private Gson gson = new Gson();
  private HttpServer server;
  private NotificationStore notificationStore;
  private ClientStore clientStore;
}
