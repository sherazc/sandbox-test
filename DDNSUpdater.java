import java.io.*;
import java.net.*;
import java.util.regex.*;
import org.json.*;

/**
 * Converted this bash script https://github.com/K0p1-Git/cloudflare-ddns-updater/blob/main/cloudflare-template.sh 
 * to java
 */

public class DDNSUpdater {
    private static final String AUTH_EMAIL = "";
    private static final String AUTH_METHOD = "token";
    private static final String AUTH_KEY = "";
    private static final String ZONE_IDENTIFIER = "";
    private static final String RECORD_NAME = "";
    private static final int TTL = 3600;
    private static final boolean PROXY = false;
    private static final String SITE_NAME = "";
    private static final String SLACK_CHANNEL = "";
    private static final String SLACK_URI = "";
    private static final String DISCORD_URI = "";

    public static void main(String[] args) throws Exception {
        String ip = getPublicIP();
        if (ip == null) {
            System.err.println("DDNS Updater: Failed to find a valid IP.");
            System.exit(2);
        }
        
        String authHeader = AUTH_METHOD.equals("global") ? "X-Auth-Key: " : "Authorization: Bearer ";
        
        String record = httpRequest("https://api.cloudflare.com/client/v4/zones/" + ZONE_IDENTIFIER + "/dns_records?type=A&name=" + RECORD_NAME, "GET", authHeader);
        
        if (record.contains("\"count\":0")) {
            System.err.println("DDNS Updater: Record does not exist, create one first?");
            System.exit(1);
        }
        
        JSONObject jsonRecord = new JSONObject(record);
        String oldIp = jsonRecord.getJSONArray("result").getJSONObject(0).getString("content");
        if (ip.equals(oldIp)) {
            System.out.println("DDNS Updater: IP (" + ip + ") for " + RECORD_NAME + " has not changed.");
            System.exit(0);
        }
        
        String recordId = jsonRecord.getJSONArray("result").getJSONObject(0).getString("id");
        JSONObject updateData = new JSONObject();
        updateData.put("type", "A");
        updateData.put("name", RECORD_NAME);
        updateData.put("content", ip);
        updateData.put("ttl", TTL);
        updateData.put("proxied", PROXY);
        
        String updateResponse = httpRequest("https://api.cloudflare.com/client/v4/zones/" + ZONE_IDENTIFIER + "/dns_records/" + recordId, "PATCH", authHeader, updateData.toString());
        if (updateResponse.contains("\"success\":false")) {
            System.err.println("DDNS Updater: Failed to update DDNS.");
            notifyServices("Update Failed", ip);
            System.exit(1);
        }
        System.out.println("DDNS Updater: IP updated successfully.");
        notifyServices("Updated", ip);
    }

    private static String getPublicIP() throws IOException {
        String[] services = {"https://api.ipify.org", "https://ipv4.icanhazip.com"};
        for (String service : services) {
            try {
                String ip = httpRequest(service, "GET", null);
                if (ip.matches("\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b")) {
                    return ip;
                }
            } catch (Exception ignored) {}
        }
        return null;
    }

    private static String httpRequest(String urlStr, String method, String authHeader) throws IOException {
        return httpRequest(urlStr, method, authHeader, null);
    }

    private static String httpRequest(String urlStr, String method, String authHeader, String data) throws IOException {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method);
        conn.setRequestProperty("Content-Type", "application/json");
        if (authHeader != null) {
            conn.setRequestProperty(authHeader.split(": ")[0], authHeader.split(": ")[1] + AUTH_KEY);
        }
        if (data != null) {
            conn.setDoOutput(true);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(data.getBytes());
            }
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            return response.toString();
        }
    }

    private static void notifyServices(String status, String ip) throws IOException {
        if (!SLACK_URI.isEmpty()) {
            JSONObject slackMessage = new JSONObject();
            slackMessage.put("channel", SLACK_CHANNEL);
            slackMessage.put("text", SITE_NAME + " DDNS " + status + ": " + RECORD_NAME + " IP: " + ip);
            httpRequest(SLACK_URI, "POST", "Content-Type: application/json", slackMessage.toString());
        }
        if (!DISCORD_URI.isEmpty()) {
            JSONObject discordMessage = new JSONObject();
            discordMessage.put("content", SITE_NAME + " DDNS " + status + ": " + RECORD_NAME + " IP: " + ip);
            httpRequest(DISCORD_URI, "POST", "Content-Type: application/json", discordMessage.toString());
        }
    }
}
