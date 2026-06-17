package com.wife.app;

import android.content.Context;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class EmojiAdapter {

    public interface OnCategoryClickListener {
        void onCategoryClick(String category);
    }

    public interface OnEmojiClickListener {
        void onEmojiClick(String emojiChar);
    }

    // Base Category Adapter
    public static class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.ViewHolder> {
        private final List<String> categories;
        private final OnCategoryClickListener listener;

        public CategoryAdapter(List<String> categories, OnCategoryClickListener listener) {
            this.categories = categories;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            final String category = categories.get(position);
            holder.textView.setText(category);
            holder.textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            holder.textView.setGravity(Gravity.CENTER);
            holder.textView.setPadding(32, 16, 32, 16);
            holder.textView.setTextColor(0xFF006495); // editorial_accent
            
            holder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onCategoryClick(category);
                }
            });
        }

        @Override
        public int getItemCount() {
            return categories.size();
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {
            TextView textView;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                textView = itemView.findViewById(android.R.id.text1);
            }
        }
    }

    // Base Grid Adapter
    public static class GridAdapter extends RecyclerView.Adapter<GridAdapter.ViewHolder> {
        private final List<EmojiLoader.EmojiDTO> emojis;
        private final OnEmojiClickListener listener;

        public GridAdapter(List<EmojiLoader.EmojiDTO> emojis, OnEmojiClickListener listener) {
            this.emojis = emojis;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            TextView textView = new TextView(parent.getContext());
            textView.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            ));
            textView.setGravity(Gravity.CENTER);
            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
            textView.setPadding(8, 8, 8, 8);
            return new ViewHolder(textView);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            final EmojiLoader.EmojiDTO emoji = emojis.get(position);
            holder.textView.setText(emoji.getEmoji());
            holder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onEmojiClick(emoji.getEmoji());
                }
            });
        }

        @Override
        public int getItemCount() {
            return emojis.size();
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {
            TextView textView;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                textView = (TextView) itemView;
            }
        }
    }
}

// Package-private class implementations to resolve direct ChatActivity references
class EmojiCategoryAdapter extends EmojiAdapter.CategoryAdapter {
    public EmojiCategoryAdapter(List<String> categories, EmojiAdapter.OnCategoryClickListener listener) {
        super(categories, listener);
    }
}

class EmojiGridAdapter extends EmojiAdapter.GridAdapter {
    public EmojiGridAdapter(List<EmojiLoader.EmojiDTO> emojis, EmojiAdapter.OnEmojiClickListener listener) {
        super(emojis, listener);
    }
}