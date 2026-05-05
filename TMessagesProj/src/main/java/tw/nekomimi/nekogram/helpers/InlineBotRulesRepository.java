/*
 * Nagram - Inline Bot Rules dual-track repository.
 *
 * Bridges remote (read-only, from InlineBotRulesHelper) and
 * local (full CRUD, persisted via NaConfig) rule lists,
 * exposing a unified RuleItem view to the UI layer and
 * sharing a bounded LRU Pattern cache for fast matching.
 */

package tw.nekomimi.nekogram.helpers;

import android.text.TextUtils;
import android.util.Base64;

import org.telegram.messenger.FileLog;
import org.telegram.tgnet.SerializedData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import tw.nekomimi.nekogram.helpers.remote.InlineBotRulesHelper;
import xyz.nextalone.nagram.NaConfig;

public class InlineBotRulesRepository {

    public static final int LRU_CAPACITY = 200;

    public enum Source {
        REMOTE,
        LOCAL
    }

    public static class RuleItem {
        public final Source source;
        public final int localIndex;
        public final String username;
        public final ArrayList<String> rules;
        public boolean enabled;

        public RuleItem(Source source, int localIndex, String username,
                        ArrayList<String> rules, boolean enabled) {
            this.source = source;
            this.localIndex = localIndex;
            this.username = username;
            this.rules = rules == null ? new ArrayList<>() : rules;
            this.enabled = enabled;
        }

        public String getTitle() {
            return username == null ? "" : username;
        }

        public String getSummary() {
            if (rules == null || rules.isEmpty()) return "";
            return TextUtils.join(" | ", rules);
        }
    }

    private static final Object LOCK = new Object();

    private static final Map<String, Pattern> PATTERN_LRU =
            Collections.synchronizedMap(new LinkedHashMap<String, Pattern>(LRU_CAPACITY, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Pattern> eldest) {
                    return size() > LRU_CAPACITY;
                }
            });

    private static final ArrayList<Runnable> LISTENERS = new ArrayList<>();

    private static ArrayList<InlineBotRulesHelper.InlineBotRule> localCache;

    public static void addListener(Runnable r) {
        if (r == null) return;
        synchronized (LISTENERS) {
            if (!LISTENERS.contains(r)) LISTENERS.add(r);
        }
    }

    public static void removeListener(Runnable r) {
        if (r == null) return;
        synchronized (LISTENERS) {
            LISTENERS.remove(r);
        }
    }

    private static void notifyChanged() {
        ArrayList<Runnable> snapshot;
        synchronized (LISTENERS) {
            snapshot = new ArrayList<>(LISTENERS);
        }
        for (Runnable r : snapshot) {
            try {
                r.run();
            } catch (Throwable t) {
                FileLog.e(t);
            }
        }
    }

    public static Pattern compile(String regex) {
        if (TextUtils.isEmpty(regex)) return null;
        Pattern p = PATTERN_LRU.get(regex);
        if (p != null) return p;
        try {
            p = Pattern.compile(regex);
            PATTERN_LRU.put(regex, p);
            return p;
        } catch (PatternSyntaxException e) {
            return null;
        }
    }

    public static boolean validateRegex(String regex) {
        if (TextUtils.isEmpty(regex)) return false;
        try {
            Pattern.compile(regex);
            return true;
        } catch (PatternSyntaxException e) {
            return false;
        }
    }

    public static void invalidatePattern(String regex) {
        if (regex != null) PATTERN_LRU.remove(regex);
    }

    public static void clearPatternCache() {
        PATTERN_LRU.clear();
    }

    public static ArrayList<RuleItem> getAll() {
        ArrayList<RuleItem> list = new ArrayList<>();
        list.addAll(getRemote());
        list.addAll(getLocal());
        return list;
    }

    public static ArrayList<RuleItem> getRemote() {
        ArrayList<RuleItem> result = new ArrayList<>();
        InlineBotRulesHelper helper = InlineBotRulesHelper.getInstance();
        Set<String> disabled = getDisabledRemoteSet();
        try {
            ArrayList<InlineBotRulesHelper.InlineBotRule> remote = helper.getRules();
            if (remote != null) {
                for (InlineBotRulesHelper.InlineBotRule r : remote) {
                    boolean enabled = !disabled.contains(r.username);
                    ArrayList<String> rules = r.rules == null
                            ? new ArrayList<>() : new ArrayList<>(r.rules);
                    result.add(new RuleItem(Source.REMOTE, -1, r.username, rules, enabled));
                }
            }
        } catch (Throwable t) {
            FileLog.e(t);
        }
        return result;
    }

    public static ArrayList<RuleItem> getLocal() {
        ArrayList<RuleItem> result = new ArrayList<>();
        ArrayList<InlineBotRulesHelper.InlineBotRule> local = loadLocal();
        ArrayList<Boolean> enabledList = loadLocalEnabledList(local.size());
        for (int i = 0; i < local.size(); i++) {
            InlineBotRulesHelper.InlineBotRule r = local.get(i);
            ArrayList<String> rules = r.rules == null
                    ? new ArrayList<>() : new ArrayList<>(r.rules);
            result.add(new RuleItem(Source.LOCAL, i, r.username, rules, enabledList.get(i)));
        }
        return result;
    }

    public static void triggerRemoteRefresh() {
        try {
            InlineBotRulesHelper.getInstance().checkInlineBotRules();
        } catch (Throwable t) {
            FileLog.e(t);
        }
        notifyChanged();
    }

    public static void setRemoteEnabled(String username, boolean enabled) {
        if (TextUtils.isEmpty(username)) return;
        Set<String> disabled = getDisabledRemoteSet();
        if (enabled) {
            disabled.remove(username);
        } else {
            disabled.add(username);
        }
        saveDisabledRemoteSet(disabled);
        notifyChanged();
    }

    public static void setLocalEnabled(int idx, boolean enabled) {
        synchronized (LOCK) {
            ArrayList<InlineBotRulesHelper.InlineBotRule> list = loadLocal();
            if (idx < 0 || idx >= list.size()) return;
            ArrayList<Boolean> enabledList = loadLocalEnabledList(list.size());
            enabledList.set(idx, enabled);
            saveLocal(list, enabledList);
        }
        notifyChanged();
    }

    public static boolean addLocal(String username, ArrayList<String> rules) {
        if (TextUtils.isEmpty(username) || rules == null || rules.isEmpty()) return false;
        for (String r : rules) {
            if (!validateRegex(r)) return false;
        }
        synchronized (LOCK) {
            ArrayList<InlineBotRulesHelper.InlineBotRule> list = loadLocal();
            ArrayList<Boolean> enabledList = loadLocalEnabledList(list.size());
            list.add(0, new InlineBotRulesHelper.InlineBotRule(username, rules));
            enabledList.add(0, true);
            saveLocal(list, enabledList);
        }
        for (String r : rules) compile(r);
        notifyChanged();
        return true;
    }

    public static boolean updateLocal(int idx, String username, ArrayList<String> rules) {
        if (idx < 0 || TextUtils.isEmpty(username) || rules == null || rules.isEmpty()) {
            return false;
        }
        for (String r : rules) {
            if (!validateRegex(r)) return false;
        }
        synchronized (LOCK) {
            ArrayList<InlineBotRulesHelper.InlineBotRule> list = loadLocal();
            if (idx >= list.size()) return false;
            ArrayList<Boolean> enabledList = loadLocalEnabledList(list.size());
            InlineBotRulesHelper.InlineBotRule old = list.get(idx);
            if (old != null && old.rules != null) {
                for (String r : old.rules) invalidatePattern(r);
            }
            list.set(idx, new InlineBotRulesHelper.InlineBotRule(username, rules));
            saveLocal(list, enabledList);
        }
        for (String r : rules) compile(r);
        notifyChanged();
        return true;
    }

    public static void removeLocal(int idx) {
        synchronized (LOCK) {
            ArrayList<InlineBotRulesHelper.InlineBotRule> list = loadLocal();
            if (idx < 0 || idx >= list.size()) return;
            ArrayList<Boolean> enabledList = loadLocalEnabledList(list.size());
            InlineBotRulesHelper.InlineBotRule old = list.remove(idx);
            enabledList.remove(idx);
            if (old != null && old.rules != null) {
                for (String r : old.rules) invalidatePattern(r);
            }
            saveLocal(list, enabledList);
        }
        notifyChanged();
    }

    public static String matchRule(String text) {
        if (!NaConfig.INSTANCE.getFixUrlAutoInlineBot().Bool()) {
            return null;
        }
        if (TextUtils.isEmpty(text)) return null;
        for (RuleItem item : getAll()) {
            if (!item.enabled) continue;
            for (String rule : item.rules) {
                Pattern p = compile(rule);
                if (p == null) continue;
                if (p.matcher(text).find()) return item.username;
            }
        }
        return null;
    }

    private static ArrayList<InlineBotRulesHelper.InlineBotRule> loadLocal() {
        synchronized (LOCK) {
            if (localCache != null) return new ArrayList<>(localCache);
            ArrayList<InlineBotRulesHelper.InlineBotRule> list = new ArrayList<>();
            String raw = NaConfig.INSTANCE.getLocalInlineBotRulesData().String();
            if (!TextUtils.isEmpty(raw)) {
                try {
                    byte[] bytes = Base64.decode(raw, Base64.DEFAULT);
                    SerializedData data = new SerializedData(bytes);
                    int n = data.readInt32(false);
                    for (int i = 0; i < n; i++) {
                        list.add(InlineBotRulesHelper.InlineBotRule.deserialize(data));
                    }
                    data.cleanup();
                } catch (Throwable t) {
                    FileLog.e(t);
                }
            }
            localCache = list;
            return new ArrayList<>(list);
        }
    }

    private static ArrayList<Boolean> loadLocalEnabledList(int size) {
        ArrayList<Boolean> result = new ArrayList<>();
        String raw = NaConfig.INSTANCE.getLocalInlineBotRulesEnabled().String();
        String[] parts = TextUtils.isEmpty(raw) ? new String[0] : raw.split(",");
        for (int i = 0; i < size; i++) {
            if (i < parts.length) {
                result.add(!"0".equals(parts[i]));
            } else {
                result.add(true);
            }
        }
        return result;
    }

    private static void saveLocal(ArrayList<InlineBotRulesHelper.InlineBotRule> list,
                                  ArrayList<Boolean> enabledList) {
        SerializedData s = new SerializedData();
        s.writeInt32(list.size());
        for (InlineBotRulesHelper.InlineBotRule r : list) {
            r.serializeToStream(s);
        }
        String encoded = Base64.encodeToString(s.toByteArray(),
                Base64.NO_WRAP | Base64.NO_PADDING);
        s.cleanup();
        NaConfig.INSTANCE.getLocalInlineBotRulesData().setConfigString(encoded);

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < enabledList.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append(enabledList.get(i) ? '1' : '0');
        }
        NaConfig.INSTANCE.getLocalInlineBotRulesEnabled().setConfigString(sb.toString());
        localCache = new ArrayList<>(list);
    }

    private static Set<String> getDisabledRemoteSet() {
        String raw = NaConfig.INSTANCE.getDisabledRemoteInlineBotRules().String();
        if (TextUtils.isEmpty(raw)) return new HashSet<>();
        return new HashSet<>(Arrays.asList(raw.split("\n")));
    }

    private static void saveDisabledRemoteSet(Set<String> set) {
        NaConfig.INSTANCE.getDisabledRemoteInlineBotRules()
                .setConfigString(TextUtils.join("\n", set));
    }
}
