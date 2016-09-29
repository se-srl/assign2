package util;

import com.google.gson.annotations.SerializedName;

public enum Severity {
  @SerializedName("notice")
  NOTICE,
  @SerializedName("caution")
  CAUTION,
  @SerializedName("urgent")
  URGENT

}
