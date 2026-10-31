package com.example.focus.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.focus.R;
import com.example.focus.model.AppItem;

import java.util.ArrayList;
import java.util.List;

public class AppDrawerAdapter extends RecyclerView.Adapter<AppDrawerAdapter.ViewHolder> {

    public interface OnAppClickListener {
        void onAppClick(AppItem app);
        void onAppLongClick(AppItem app);
    }

    private final List<AppItem> fullList = new ArrayList<>();
    private final List<AppItem> filteredList = new ArrayList<>();
    private final OnAppClickListener listener;
    private boolean isDarkMode = true;

    public AppDrawerAdapter(OnAppClickListener listener) {
        this.listener = listener;
    }

    public void setDarkMode(boolean darkMode) {
        this.isDarkMode = darkMode;
        notifyDataSetChanged();
    }

    public void setApps(List<AppItem> apps) {
        fullList.clear();
        if (apps != null) {
            fullList.addAll(apps);
        }
        filteredList.clear();
        filteredList.addAll(fullList);
        notifyDataSetChanged();
    }

    public int filter(String query) {
        filteredList.clear();
        if (query == null || query.trim().isEmpty()) {
            filteredList.addAll(fullList);
        } else {
            String q = query.trim().toLowerCase();
            for (AppItem item : fullList) {
                String display = item.getDisplayLabel().toLowerCase();
                String original = item.getOriginalLabel().toLowerCase();
                if (display.contains(q) || original.contains(q)) {
                    filteredList.add(item);
                }
            }
        }
        notifyDataSetChanged();
        return filteredList.size();
    }

    public AppItem getFirstVisibleApp() {
        return filteredList.isEmpty() ? null : filteredList.get(0);
    }

    public int getFilteredCount() {
        return filteredList.size();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_app_drawer, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AppItem app = filteredList.get(position);
        holder.appLabel.setText(app.getDisplayLabel());
        holder.appLabel.setTextColor(isDarkMode ? 0xFFFFFFFF : 0xFF111111);
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onAppClick(app);
        });
        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) listener.onAppLongClick(app);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return filteredList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView appLabel;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            appLabel = itemView.findViewById(R.id.app_label);
        }
    }
}

