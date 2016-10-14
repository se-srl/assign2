package server;

import java.io.IOException;

import util.Config;

public class Driver {
  /**
   * Instantiates a MitterServer to which requests can be sent almost immediately.
   * @param args the port number to use
   */
  public static void main(String[] args) {
    try {
      MitterServer server = new MitterServer(new Config(args[0]));
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
