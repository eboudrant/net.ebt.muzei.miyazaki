package net.ebt.muzei.miyazaki.model;

import com.google.gson.annotations.SerializedName;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by eboudrant on 10/1/14.
 */
public class Artwork {

  public final Map<String, Float> colors = new HashMap<String, Float>();

  @SerializedName("aid")
  public String aid;
  @SerializedName("hash")
  public String hash;
  @SerializedName("url")
  public String url;
  @SerializedName("width")
  public int width;
  @SerializedName("height")
  public int height;
  @SerializedName("ratio")
  public float ratio;
  @SerializedName("caption")
  public String caption;
  @SerializedName("subtitle")
  public String subtitle;

  @SerializedName("silver")
  public float silver;
  @SerializedName("grey")
  public float grey;
  @SerializedName("black")
  public float black;
  @SerializedName("maroon")
  public float maroon;
  @SerializedName("orange")
  public float orange;
  @SerializedName("yellow")
  public float yellow;
  @SerializedName("olive")
  public float olive;
  @SerializedName("lime")
  public float lime;
  @SerializedName("green")
  public float green;
  @SerializedName("aqua")
  public float aqua;
  @SerializedName("teal")
  public float teal;
  @SerializedName("blue")
  public float blue;
  @SerializedName("navy")
  public float navy;
  @SerializedName("fuchsia")
  public float fuchsia;
  @SerializedName("purple")
  public float purple;
  @SerializedName("white")
  public float white;

  public void done() {
    colors.put("silver", silver);
    colors.put("grey", grey);
    colors.put("black", black);
    colors.put("maroon", maroon);
    colors.put("orange", orange);
    colors.put("yellow", yellow);
    colors.put("olive", olive);
    colors.put("lime", lime);
    colors.put("green", green);
    colors.put("aqua", aqua);
    colors.put("teal", teal);
    colors.put("blue", blue);
    colors.put("navy", navy);
    colors.put("fuchsia", fuchsia);
    colors.put("purple", purple);
    colors.put("white", white);
  }
}
