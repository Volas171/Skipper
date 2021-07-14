package mindurka;

import arc.files.Fi;
import arc.util.serialization.Json;
import arc.util.serialization.JsonValue;
import arc.util.serialization.JsonWriter;
import org.json.JSONObject;

import java.io.*;
import java.net.URISyntaxException;
import java.nio.file.FileSystemException;
import java.util.Collection;
import java.util.Map;


public class ConfigurationManager {
    private Fi jsonFile;
    private Json json;

    public ConfigurationManager(String configPath, String fileName) throws IOException, URISyntaxException {
        init(configPath, fileName);
    }

    public ConfigurationManager(String fileName) throws IOException, URISyntaxException {
        init("config/mods/OhNo/", fileName);
    }

    public ConfigurationManager() throws IOException, URISyntaxException {
        init("config/mods/OhNo/", "settings.json");
    }


    public JSONObject getJsonData() {
        JsonValue ДерьмоОтАнюка = json.fromJson(null, jsonFile);
        return new JSONObject(ДерьмоОтАнюка.toJson(JsonWriter.OutputType.json));
    }

    public void saveJsonData(JSONObject jsonData) {
        String stringJson = jsonData.toString(1);
        jsonFile.writeString(stringJson);
    }

    private void init(String configPath, String configName) throws IOException {
        File configurationFile = new File(configPath, configName);
        if (!configurationFile.getParentFile().exists()) {
            boolean isCreated = configurationFile.getParentFile().mkdirs();
            if (!isCreated) throw new FileSystemException(configPath);
        }

        jsonFile = new Fi(configurationFile);
        json = new Json(JsonWriter.OutputType.json);

        if (!configurationFile.exists()) {
            InputStream in = getClass().getResourceAsStream("/config/" + configName);

            if (in == null) throw new FileNotFoundException("resources:///config/" + configName);

            BufferedReader reader = new BufferedReader(new InputStreamReader(in));

            String line;
            while ((line = reader.readLine()) != null) {
                jsonFile.writeString(line + "\n", true);
            }
        }
    }

    public void setJsonValue(JSONObject jsonData, String name, String value) {
        jsonData.remove(name);
        jsonData.put(name, value);
    }

    public void setJsonValue(JSONObject jsonData, String name, int value) {
        jsonData.remove(name);
        jsonData.put(name, value);
    }

    public void setJsonValue(JSONObject jsonData, String name, long value) {
        jsonData.remove(name);
        jsonData.put(name, value);
    }

    public void setJsonValue(JSONObject jsonData, String name, float value) {
        jsonData.remove(name);
        jsonData.put(name, value);
    }

    public void setJsonValue(JSONObject jsonData, String name, double value) {
        jsonData.remove(name);
        jsonData.put(name, value);
    }

    public void setJsonValue(JSONObject jsonData, String name, Object value) {
        jsonData.remove(name);
        jsonData.put(name, value);
    }

    public void setJsonValue(JSONObject jsonData, String name, boolean value) {
        jsonData.remove(name);
        jsonData.put(name, value);
    }

    public <T, V> void setJsonValue(JSONObject jsonData, String name, Map<T, V> value) {
        jsonData.remove(name);
        jsonData.put(name, value);
    }

    public <T> void setJsonValue(JSONObject jsonData, String name, Collection<T> value) {
        jsonData.remove(name);
        jsonData.put(name, value);
    }
}
