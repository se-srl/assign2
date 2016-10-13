package util;

import com.sun.org.apache.bcel.internal.generic.RET;

import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

public class Config {
  static final String CONFIG_FILE = "mitter.properties";

  static final String HOSTNAME = "hostname";
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

  public Config() {
    this(CONFIG_FILE);
  }

  public Config(String configFile) {
    try {
      properties.load(new FileReader(configFile));
    } catch (IOException e) {
      throw new RuntimeException("Failed to load config file" + configFile + ".");
    }
  }

  public String getHostname() {
    return properties.getProperty(HOSTNAME, "localhost");
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
