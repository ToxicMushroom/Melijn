package com.pixelatedsource.jda.utils;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONObject;

import java.io.IOException;

public class WebUtils {

    private static OkHttpClient client = new OkHttpClient();

    public static String run(String url) {
        Request request = new Request.Builder()
                .url(url)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response != null && response.body() != null)
            return response.body().string();
            else return "error";
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "IOException";
    }

    public static String getDogUrl() {
        JSONObject jsonObject = new JSONObject(run("https://api.thedogapi.co.uk/v2/dog.php"));
        return ((JSONObject) jsonObject.getJSONArray("data").get(0)).get("url").toString();
    }

    //Weeb.sh api
    public static String getPatatoUrl() {
        JSONObject jsonObject = new JSONObject(run("https://rra.ram.moe/i/r?type=potato"));
        return jsonObject.get("path").toString().replaceFirst("/i", "https://cdn.ram.moe");
    }

}
