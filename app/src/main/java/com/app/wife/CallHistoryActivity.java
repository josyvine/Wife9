package com.wife.app;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.wife.app.databinding.ActivityCallHistoryBinding;

import java.util.ArrayList;
import java.util.List;

public class CallHistoryActivity extends AppCompatActivity {

    private ActivityCallHistoryBinding binding;
    private CallLogsAdapter adapter;
    private final List<CallEntity> logsList = new ArrayList<>();
    private RoomDatabaseManager db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCallHistoryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = RoomDatabaseManager.getInstance(this);

        setupToolbar();
        setupRecyclerView();
        loadLogs();
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbarCallHistory);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        binding.toolbarCallHistory.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupRecyclerView() {
        adapter = new CallLogsAdapter(logsList);
        binding.rvCallHistory.setLayoutManager(new LinearLayoutManager(this));
        binding.rvCallHistory.setAdapter(adapter);
    }

    private void loadLogs() {
        List<CallEntity> logs = db.callDao().getAllCalls();
        logsList.clear();
        logsList.addAll(logs);
        
        if (logsList.isEmpty()) {
            binding.tvCallNoHistory.setVisibility(View.VISIBLE);
        } else {
            binding.tvCallNoHistory.setVisibility(View.GONE);
        }
        adapter.notifyDataSetChanged();
    }

    // --- Embedded Symmetrical RecyclerView structures ---
    private static class CallLogsAdapter extends RecyclerView.Adapter<CallLogsAdapter.ViewHolder> {
        private final List<CallEntity> data;

        public CallLogsAdapter(List<CallEntity> data) {
            this.data = data;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            CallEntity entry = data.get(position);
            holder.text1.setText("Call with: " + entry.getPeer() + " (" + entry.getType() + ")");
            holder.text2.setText("Duration: " + Utils.formatDuration(entry.getDuration()) + " | " + Utils.formatDate(entry.getTimestamp()));
            
            // Custom styling for readability
            holder.text1.setTextColor(0xFF333333);
            holder.text2.setTextColor(0xFF888888);
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {
            TextView text1;
            TextView text2;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                text1 = itemView.findViewById(android.R.id.text1);
                text2 = itemView.findViewById(android.R.id.text2);
                
                // Set standard padding for simple list item 2
                itemView.setPadding(32, 24, 32, 24);
            }
        }
    }
}
