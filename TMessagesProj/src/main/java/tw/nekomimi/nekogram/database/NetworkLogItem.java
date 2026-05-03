package tw.nekomimi.nekogram.database;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;

@Entity
public class NetworkLogItem {

    @Id
    public long id;

    public long timestamp;
    public String method;
    public String url;
    public int statusCode;
    public long responseTime;
    public String requestHeaders;
    public String requestBody;
    public String requestParams;
    public String responseHeaders;
    public String responseBody;
    public String errorMessage;

    public NetworkLogItem() {
    }

    public NetworkLogItem(long timestamp, String method, String url, int statusCode, long responseTime,
                          String requestHeaders, String requestBody, String requestParams,
                          String responseHeaders, String responseBody, String errorMessage) {
        this.timestamp = timestamp;
        this.method = method;
        this.url = url;
        this.statusCode = statusCode;
        this.responseTime = responseTime;
        this.requestHeaders = requestHeaders;
        this.requestBody = requestBody;
        this.requestParams = requestParams;
        this.responseHeaders = responseHeaders;
        this.responseBody = responseBody;
        this.errorMessage = errorMessage;
    }
}
