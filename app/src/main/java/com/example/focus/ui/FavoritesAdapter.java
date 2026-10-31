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

public class FavoritesAdapter extends RecyclerView.Adapter<FavoritesAdapter.ViewHolder> {

    public interface OnFavoriteClickListener {
        void onFavoriteClick(AppItem app);
        void onFavoriteLongClick(AppItem app);
    }

    private final List<AppItem> favorites = new ArrayList<>();
    private final OnFavoriteClickListener listener;
    private boolean isDarkMode = true;

    public FavoritesAdapter(OnFavoriteClickListener listener) {
        this.listener = listener;
    }

    public void setDarkMode(boolean darkMode) {
        this.isDarkMode = darkMode;
        notifyDataSetChanged();
    }

    public void setFavorites(List<AppItem> list) {
        favorites.clear();
        if (list != null) {
            favorites.addAll(list);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_favorite, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AppItem app = favorites.get(position);
        holder.favoriteLabel.setText(app.getDisplayLabel());
        holder.favoriteLabel.setTextColor(isDarkMode ? 0xFFFFFFFF : 0xFF111111);
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onFavoriteClick(app);
        });
        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) listener.onFavoriteLongClick(app);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return favorites.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView favoriteLabel;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            favoriteLabel = itemView.findViewById(R.id.favorite_label);
        }
    }
}

