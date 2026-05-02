package xyz.nextalone.nagram.network;

import android.content.Context;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import tw.nekomimi.nekogram.database.NetworkLogItem;

public class NetworkLogAdapter extends RecyclerView.Adapter<NetworkLogAdapter.NetworkLogViewHolder> {

    private Context context;
    private NetworkLogActivity parent;
    private List<NetworkLogItem> logs = new ArrayList<>();

    public NetworkLogAdapter(Context context, NetworkLogActivity parent) {
        this.context = context;
        this.parent = parent;
    }

    public void setLogs(List<NetworkLogItem> logs) {
        this.logs = logs;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return 0;
    }

    @Override
    public int getItemCount() {
        return logs.size();
    }

    @NonNull
    @Override
    public NetworkLogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        NetworkLogCell cell = new NetworkLogCell(context);
        return new NetworkLogViewHolder(cell);
    }

    @Override
    public void onBindViewHolder(@NonNull NetworkLogViewHolder holder, int position) {
        holder.bind(logs.get(position));
    }

    @Override
    public long getItemId(int position) {
        return logs.get(position).id;
    }

    @Override
    public void onViewRecycled(@NonNull NetworkLogViewHolder holder) {
        super.onViewRecycled(holder);
        holder.recycle();
    }

    public void onItemClick(int position) {
        if (parent != null && position >= 0 && position < logs.size()) {
            parent.openLogDetail(logs.get(position));
        }
    }

    public static class NetworkLogViewHolder extends RecyclerView.ViewHolder {
        private final NetworkLogCell cell;

        public NetworkLogViewHolder(NetworkLogCell cell) {
            super(cell);
            this.cell = cell;
        }

        public void bind(NetworkLogItem item) {
            cell.bind(item);
        }

        public void recycle() {
            // recycle resources if needed
        }
    }
}
