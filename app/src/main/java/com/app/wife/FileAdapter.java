package com.wife.app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class FileAdapter extends RecyclerView.Adapter<FileAdapter.ViewHolder> {

    private final List<FileEntity> files;

    public FileAdapter(List<FileEntity> files) {
        this.files = files;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_file, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FileEntity file = files.get(position);
        holder.tvName.setText(file.getFilename());
        holder.tvSize.setText(Utils.formatFileSize(file.getSize()));
        holder.tvTime.setText(Utils.formatDate(file.getTimestamp()));
    }

    @Override
    public int getItemCount() {
        return files.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;
        TextView tvSize;
        TextView tvTime;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvFileName);
            tvSize = itemView.findViewById(R.id.tvFileSize);
            tvTime = itemView.findViewById(R.id.tvFileTime);
        }
    }
}
