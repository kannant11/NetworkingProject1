package common;

import merrimackutil.json.JsonIO;
import merrimackutil.json.types.JSONArray;
import merrimackutil.json.types.JSONObject;

import java.nio.file.*;
import java.util.*;

public class AccountsDb {
    private final Map<String, Account> byUser = new HashMap<>();

    public static AccountsDb load(Path path) throws Exception {
        JSONObject root = (JSONObject) JsonIO.readObject(path.toFile());
        JSONArray arr = root.getArray("accounts");
        AccountsDb db = new AccountsDb();
        for (int i = 0; i < arr.size(); i++) {
            JSONObject o = arr.getObject(i);
            Account a = new Account(
                    o.getString("username"),
                    o.getString("password"),
                    o.getString("spool"));
            db.byUser.put(a.username(), a);
        }
        return db;
    }

    public Account get(String username) { return byUser.get(username); }
}
