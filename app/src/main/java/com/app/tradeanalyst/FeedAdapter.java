package com.tradeanalyst.app;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class FeedAdapter extends RecyclerView.Adapter<FeedAdapter.FeedViewHolder> {

    private List<FeedItem> mItems = new ArrayList<>();

    public void setItems(List<FeedItem> items) {
        mItems = items;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public FeedViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_market_item, parent, false);
        return new FeedViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FeedViewHolder holder, int position) {
        FeedItem item = mItems.get(position);
        holder.titleText.setText(item.title);
        holder.subtitleText.setText(item.subtitle);
        holder.badgeText.setText(item.badge);

        // Render circular indicator color dynamically with standard drawable shape
        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.OVAL);
        shape.setColor(item.indicatorColor);
        holder.indicatorView.setBackground(shape);

        // If Buy or positive news
        if (item.badge.contains("BUY") || item.badge.contains("UP") || item.badge.contains("%") && item.indicatorColor == Color.parseColor("#10B981")) {
            holder.badgeText.setTextColor(Color.parseColor("#10B981"));
        } else if (item.badge.contains("SELL") || item.badge.contains("DOWN") || item.indicatorColor == Color.parseColor("#EF4444")) {
            holder.badgeText.setTextColor(Color.parseColor("#EF4444"));
        } else {
            holder.badgeText.setTextColor(Color.parseColor("#14B8A6"));
        }
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    static class FeedViewHolder extends RecyclerView.ViewHolder {
        View indicatorView;
        TextView titleText;
        TextView subtitleText;
        TextView badgeText;

        FeedViewHolder(View itemView) {
            super(itemView);
            indicatorView = itemView.findViewById(R.id.row_indicator_circle);
            titleText = itemView.findViewById(R.id.row_title);
            subtitleText = itemView.findViewById(R.id.row_subtitle);
            badgeText = itemView.findViewById(R.id.row_badge);
        }
    }
}
