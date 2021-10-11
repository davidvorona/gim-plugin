package com.gimp;

import lombok.extern.slf4j.Slf4j;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.*;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;
import com.google.gson.Gson;

@Slf4j
public class LocationBroadcastManager
{
    final HttpClient client;

    final String localName;

    public HttpResponse<String> response;

    public LocationBroadcastManager(String localGimpName)
    {
        localName = localGimpName;
        client = HttpClient.newHttpClient();
    }

    public static Map<String, Map<GIMPLocationManager.Coordinate, Integer>> spoofPingData()
    {
        Map<String, Map<GIMPLocationManager.Coordinate, Integer>> data = new HashMap<>();
        Map<GIMPLocationManager.Coordinate, Integer> gimpData = new HashMap<>();
        gimpData.put(GIMPLocationManager.Coordinate.x, 100);
        gimpData.put(GIMPLocationManager.Coordinate.y, 100);
        gimpData.put(GIMPLocationManager.Coordinate.plane, 0);
        data.put("Manogram", gimpData);
        return data;
    }

    /**
     * Pings server for location of fellow GIMPs
     * @return String
     */
    public Map<String, Map<GIMPLocationManager.Coordinate, Integer>> ping() throws ExecutionException, InterruptedException, URISyntaxException
    {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI("https://postman-echo.com/get"))
                .timeout(Duration.of(5, ChronoUnit.SECONDS))
                .GET()
                .build();
        HttpResponse<String> response = client
                .sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .get();
        String body = response.body();
        log.info(body);
        // spoof response
        return LocationBroadcastManager.spoofPingData();
    }

    /**
     * Broadcasts local GIMP's current location to server
     * @param location map of player's location coordinates in x, y, plane
     */
    public void broadcast(Map<GIMPLocationManager.Coordinate, Integer> location) throws URISyntaxException, ExecutionException, InterruptedException
    {
        Map<Object, Object> body = new HashMap<>(location);
        body.put("name", localName);
        Gson gson = new Gson();
        String bodyJson = gson.toJson(body);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI("https://postman-echo.com/post"))
                .timeout(Duration.of(5, ChronoUnit.SECONDS))
                .POST(HttpRequest.BodyPublishers.ofString(bodyJson))
                .build();
        response = client
                .sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .get();
        log.info(response.body());
    }
}
