package util;

import java.util.LinkedHashMap;
import java.util.Map;

public class ExpiringLinkedHashMap<K, V> extends LinkedHashMap<K, V> {
  public ExpiringLinkedHashMap(int max_entries) {
    this.max_entries = max_entries;
  }

  @Override
  protected boolean removeEldestEntry(Map.Entry eldest) {
    return size() > max_entries;
  }

  private int max_entries;
}
