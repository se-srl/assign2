package util;

import com.google.gson.annotations.SerializedName;

public enum Severity {
  @SerializedName("notice")
  NOTICE,
  @SerializedName("caution")
  CAUTION,
  @SerializedName("urgent")
  URGENT;

  public static Severity fromString(String severity) {
    switch (severity) {
      case "notice":
        return NOTICE;
      case "caution":
        return CAUTION;
      case "urgent":
        return URGENT;
      default:
        throw new IllegalArgumentException("Expected one of notice, caution and urgent");
    }
  }

}
