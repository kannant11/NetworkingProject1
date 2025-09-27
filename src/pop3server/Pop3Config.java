package pop3server;

import common.JsonConfig;
import merrimackutil.json.types.JSONObject;

public class Pop3Config extends JsonConfig {
    public String accounts;

    public String getAccounts() {
        return accounts;
    }

    public static Pop3Config fromJson(JSONObject obj) {
        Pop3Config c = new Pop3Config();
        JsonConfig base = JsonConfig.fromJson(obj);
        c.spool = base.spool;
        c.server_name = base.server_name;
        c.port = base.port;
        c.log = base.log;
        c.accounts = obj.getString("accounts");
        return c;
    }
}
