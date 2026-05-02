package xyz.nextalone.nagram.network;

import static tw.nekomimi.nekogram.database.ObjectBoxKt.mkDatabase;

import io.objectbox.Box;
import io.objectbox.BoxStore;
import io.objectbox.query.Query;
import tw.nekomimi.nekogram.database.NetworkLogItem;
import tw.nekomimi.nekogram.database.NetworkLogItem_;
import tw.nekomimi.nekogram.utils.UIUtil;

import java.util.List;

public class NetworkLogDb {

    private static final int MAX_LOGS = 1000;
    private static BoxStore db;
    private static Box<NetworkLogItem> box;

    public static synchronized void init() {
        if (db == null) {
            db = mkDatabase("network_logs");
            box = db.boxFor(NetworkLogItem.class);
        }
    }

    private static Box<NetworkLogItem> getBox() {
        if (db == null) {
            init();
        }
        return box;
    }

    public static void save(NetworkLogItem item) {
        UIUtil.runOnIoDispatcher(() -> {
            Box<NetworkLogItem> box = getBox();
            box.put(item);

            long count = box.count();
            if (count > MAX_LOGS) {
                Query<NetworkLogItem> query = box.query()
                        .orderDesc(NetworkLogItem_.timestamp)
                        .build();
                List<NetworkLogItem> allLogs = query.find();
                if (allLogs.size() > MAX_LOGS) {
                    List<NetworkLogItem> toDelete = allLogs.subList(MAX_LOGS, allLogs.size());
                    for (NetworkLogItem log : toDelete) {
                        box.remove(log);
                    }
                }
                query.close();
            }
        });
    }

    public static List<NetworkLogItem> getAll() {
        return getBox().query()
                .orderDesc(NetworkLogItem_.timestamp)
                .build()
                .find();
    }

    public static List<NetworkLogItem> search(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAll();
        }

        int statusCode = parseStatusCode(keyword);

        if (statusCode >= 0) {
            return getBox().query()
                    .contains(NetworkLogItem_.url, keyword, io.objectbox.query.QueryBuilder.StringOrder.CASE_INSENSITIVE)
                    .or()
                    .contains(NetworkLogItem_.method, keyword, io.objectbox.query.QueryBuilder.StringOrder.CASE_INSENSITIVE)
                    .or()
                    .contains(NetworkLogItem_.responseBody, keyword, io.objectbox.query.QueryBuilder.StringOrder.CASE_INSENSITIVE)
                    .or()
                    .equal(NetworkLogItem_.statusCode, (long) statusCode)
                    .orderDesc(NetworkLogItem_.timestamp)
                    .build()
                    .find();
        } else {
            return getBox().query()
                    .contains(NetworkLogItem_.url, keyword, io.objectbox.query.QueryBuilder.StringOrder.CASE_INSENSITIVE)
                    .or()
                    .contains(NetworkLogItem_.method, keyword, io.objectbox.query.QueryBuilder.StringOrder.CASE_INSENSITIVE)
                    .or()
                    .contains(NetworkLogItem_.responseBody, keyword, io.objectbox.query.QueryBuilder.StringOrder.CASE_INSENSITIVE)
                    .orderDesc(NetworkLogItem_.timestamp)
                    .build()
                    .find();
        }
    }

    private static int parseStatusCode(String keyword) {
        try {
            return Integer.parseInt(keyword.trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public static void clearAll() {
        UIUtil.runOnIoDispatcher(() -> getBox().removeAll());
    }

    public static long getCount() {
        return getBox().count();
    }

    public static void close() {
        if (db != null) {
            db.close();
            db = null;
        }
    }
}
