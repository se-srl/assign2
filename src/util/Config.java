package util;

import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Config {
  static final String CONFIG_FILE = "mitter.properties";

  static final String MITTER_HOSTNAME = "hostname.mitter";
  static final String BROADCAST_HOSTNAME = "hostname.broadcast";
  static final String FETCH_PORT = "port.fetch";
  static final String BROADCAST_PORT = "port.broadcast";
  static final String RETRIES = "retries";
  static final String TIMEOUT = "timeout";
  static final String CAUTION_INTERVAL = "interval.caution";
  static final String NOTICE_INTERVAL = "interval.notice";
  static final String SAVE_INTERVAL = "interval.save";
  static final String URGENT_MAX = "max.urgent";
  static final String CAUTION_MAX = "max.caution";
  static final String NOTICE_MAX = "max.notice";
  static final String NOTIFICATION_SAVE_FILE = "file.notification";
  static final String SUBSCRIPTION_SAVE_FILE = "file.subscription";
  static final String MTU = "mtu";

  public Config() {
    this(CONFIG_FILE);
  }

  public Config(String configFile) {
    try {
//      final InputStream stream = this.getClass().getClassLoader().getResourceAsStream(configFile);
      properties.load(new FileReader(configFile));
    } catch (IOException e) {
      e.printStackTrace();
      throw new RuntimeException("Failed to load config file" + configFile + ".");
    }
  }

  public int getMTU() {
    return Integer.parseInt(properties.getProperty(MTU, "1500"));
  }

  public String getNotificationSaveFile() {
    return properties.getProperty(NOTIFICATION_SAVE_FILE);
  }

  public String getSubscriptionSaveFile() {
    return properties.getProperty(SUBSCRIPTION_SAVE_FILE);
  }

  public String getFetchHostname() {
    return properties.getProperty(MITTER_HOSTNAME, "localhost");
  }

  public String getBroadcastHostname() {
    return properties.getProperty(BROADCAST_HOSTNAME, "localhost");
  }

  public int getFetchPort() {
    return Integer.parseInt(properties.getProperty(FETCH_PORT, "8080"));
  }

  public int getBroadcastPort() {
    return Integer.parseInt(properties.getProperty(BROADCAST_PORT, "9090"));
  }

  public int getRetries() {
    return Integer.parseInt(properties.getProperty(RETRIES, "3"));
  }

  public int getTimeout() {
    return Integer.parseInt(properties.getProperty(TIMEOUT, "1000"));
  }

  public int getCautionInterval() {
    return Integer.parseInt(properties.getProperty(CAUTION_INTERVAL, "1"));
  }

  public int getNoticeInterval() {
    return Integer.parseInt(properties.getProperty(NOTICE_INTERVAL, "30"));
  }

  public int getSaveInterval() {
    return Integer.parseInt(properties.getProperty(SAVE_INTERVAL, "5"));
  }

  public int getCautionMax() {
    return Integer.parseInt(properties.getProperty(CAUTION_MAX, "100"));
  }

  public int getNoticeMax() {
    return Integer.parseInt(properties.getProperty(NOTICE_MAX, "500"));
  }

  public int getUrgentMax() {
    return Integer.parseInt(properties.getProperty(URGENT_MAX, "1000"));
  }

  private Properties properties;
}
