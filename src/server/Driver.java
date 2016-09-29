package server;

import java.io.IOException;

public class Driver {
  /**
   * Instantiates a MitterServer to which requests can be sent almost immediately.
   * @param args the port number to use
   */
  public static void main(String[] args) {
    int port = Integer.parseInt(args[0]);
    try {
      MitterServer server = new MitterServer("localhost", port);
      server.init();
      server.start();
      System.out.println("Server started successfully");
    } catch (IOException ioException) {
      System.err.println("Server failed to start");
      System.err.println(ioException.getMessage());
      ioException.printStackTrace();
    }
  }
}
