package common;

import merrimackutil.json.types.JSONObject;

/**
 * The `JsonConfig` class represents a configuration object with fields for spool, server name, port,
 * and log, and provides a method to create an instance from a JSON object.
 */
public class JsonConfig {
    public String spool;
    public String server_name; // maps to "server-name"
    public int port;
    public String log;

    public String getSpool() {
        return spool;
    }

    public String getServer_name() {
        return server_name;
    }

    public int getPort() {
        return port;
    }

    public String getLog() {
        return log;
    }

    public static JsonConfig fromJson(JSONObject obj) {
        JsonConfig c = new JsonConfig();
        c.spool = obj.getString("spool");
        c.server_name = obj.getString("serverName");
        c.port = obj.getInt("port");
        c.log = obj.getString("log");
        return c;
    }


}
