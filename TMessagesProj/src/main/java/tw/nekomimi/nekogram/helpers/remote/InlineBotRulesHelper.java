package tw.nekomimi.nekogram.helpers.remote;

import android.text.TextUtils;

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
        ArrayList<InlineBotRule> tmpInlineBotRules = new ArrayList<>();
        if (!TextUtils.isEmpty(list)) {
            String[] lines = list.split("\\r?\\n");
            for (String line : lines) {
                line = line.trim();
                if (TextUtils.isEmpty(line) || line.startsWith("#")) {
                    continue;
                }
                int separator = line.lastIndexOf("=>");
                if (separator <= 0) {
                    continue;
                }
                String rule = line.substring(0, separator).trim();
                String username = line.substring(separator + 2).trim();
                if (username.startsWith("@")) {
                    username = username.substring(1);
                }
                if (TextUtils.isEmpty(rule) || TextUtils.isEmpty(username)) {
                    continue;
                }
                try {
                    tmpInlineBotRules.add(new InlineBotRule(username, rule));
                } catch (RuntimeException ignored) {
                }
            }
        }
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

    public static class InlineBotRule {
        public String username;
        public String rule;
        public Pattern regexPattern;

        public InlineBotRule() {}

        public InlineBotRule(String username, String rule) {
            this.username = username;
            this.rule = rule;
            this.buildRegexPattern();
        }

        public void buildRegexPattern() {
            this.regexPattern = Pattern.compile(rule, Pattern.CASE_INSENSITIVE);
        }
    }
}
