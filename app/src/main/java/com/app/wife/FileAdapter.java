package com.wife.app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class FileAdapter extends RecyclerView.Adapter<FileAdapter.ViewHolder> {

    private final List<FileEntity> files;
    private final OnFileDeleteListener deleteListener;

    // Interface callback to relay delete button clicks to the hosting activity
    public interface OnFileDeleteListener {
        void onFileDelete(FileEntity file, int position);
    }

    public FileAdapter(List<FileEntity> files, OnFileDeleteListener deleteListener) {
        this.files = files;
        this.deleteListener = deleteListener;
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

        holder.btnDelete.setOnClickListener(v -> {
            if (deleteListener != null) {
                int adapterPos = holder.getAdapterPosition();
                if (adapterPos != RecyclerView.NO_POSITION) {
                    deleteListener.onFileDelete(files.get(adapterPos), adapterPos);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return files.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;
        TextView tvSize;
        TextView tvTime;
        ImageView btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvFileName);
            tvSize = itemView.findViewById(R.id.tvFileSize);
            tvTime = itemView.findViewById(R.id.tvFileTime);
            btnDelete = itemView.findViewById(R.id.btnDeleteFile);
        }
    }
}