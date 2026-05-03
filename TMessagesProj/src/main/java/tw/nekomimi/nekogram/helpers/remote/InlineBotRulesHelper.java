package tw.nekomimi.nekogram.helpers.remote;

import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.telegram.messenger.FileLog;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import xyz.nextalone.nagram.NaConfig;

public class InlineBotRulesHelper {
    private static volatile InlineBotRulesHelper Instance;
    private final ArrayList<InlineBotRule> inlineBotRules = new ArrayList<>();
    private String loadedRules;

    public static InlineBotRulesHelper getInstance() {
        InlineBotRulesHelper localInstance = Instance;
        if (localInstance == null) {
            synchronized (InlineBotRulesHelper.class) {
                localInstance = Instance;
                if (localInstance == null) {
                    Instance = localInstance = new InlineBotRulesHelper();
                }
                return localInstance;
            }
        }
        return localInstance;
    }

    private void loadInlineBotRules() {
        String list = NaConfig.INSTANCE.getFixUrlAutoInlineBotRules().String();
        if (TextUtils.equals(loadedRules, list)) {
            return;
        }
        loadedRules = list;
        ArrayList<InlineBotRule> tmpInlineBotRules = parseInlineBotRules(list, true);
        inlineBotRules.clear();
        inlineBotRules.addAll(tmpInlineBotRules);
    }

    public String doRegex(String textToCheck) {
        if (!NaConfig.INSTANCE.getFixUrlAutoInlineBot().Bool()) {
            return null;
        }
        if (textToCheck == null || textToCheck.isEmpty()) {
            return null;
        }
        loadInlineBotRules();
        for (InlineBotRule rule : inlineBotRules) {
            Matcher matcher = rule.regexPattern.matcher(textToCheck);
            if (matcher.find()) {
                return rule.username;
            }
        }
        return null;
    }

    public static ArrayList<InlineBotRule> parseInlineBotRules(String list, boolean compilePattern) {
        ArrayList<InlineBotRule> rules = new ArrayList<>();
        if (TextUtils.isEmpty(list)) {
            return rules;
        }
        try {
            JSONArray array = new JSONArray(list.trim());
            for (int i = 0; i < array.length(); i++) {
                JSONObject object = array.getJSONObject(i);
                addInlineBotRule(
                        rules,
                        object.optString("bot", object.optString("username", "")),
                        object.optString("pattern", object.optString("rule", "")),
                        compilePattern
                );
            }
        } catch (JSONException e) {
            FileLog.e(e);
        }
        return rules;
    }

    public static String normalizeInlineBotUsername(String username) {
        username = username == null ? "" : username.trim();
        if (username.startsWith("@")) {
            return username.substring(1);
        }
        return username;
    }

    private static void addInlineBotRule(ArrayList<InlineBotRule> rules, String username, String rule, boolean compilePattern) {
        username = normalizeInlineBotUsername(username);
        if (TextUtils.isEmpty(rule) || TextUtils.isEmpty(username)) {
            return;
        }
        try {
            rules.add(new InlineBotRule(username, rule, compilePattern));
        } catch (RuntimeException e) {
            FileLog.e(e);
        }
    }

    public static String serializeInlineBotRules(ArrayList<InlineBotRule> rules) {
        JSONArray array = new JSONArray();
        for (InlineBotRule rule : rules) {
            if (rule == null || TextUtils.isEmpty(rule.rule) || TextUtils.isEmpty(rule.username)) {
                continue;
            }
            JSONObject object = new JSONObject();
            try {
                object.put("pattern", rule.rule);
                object.put("bot", normalizeInlineBotUsername(rule.username));
                array.put(object);
            } catch (JSONException e) {
                FileLog.e(e);
            }
        }
        return array.toString();
    }

    public static class InlineBotRule {
        public String username;
        public String rule;
        public Pattern regexPattern;

        public InlineBotRule() {}

        public InlineBotRule(String username, String rule) {
            this(username, rule, true);
        }

        public InlineBotRule(String username, String rule, boolean compilePattern) {
            this.username = username;
            this.rule = rule;
            if (compilePattern) {
                this.buildRegexPattern();
            }
        }

        public void buildRegexPattern() {
            this.regexPattern = Pattern.compile(rule, Pattern.CASE_INSENSITIVE);
        }
    }
}
