package com.example.cs360weighttracker.features.weight;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cs360weighttracker.R;
import com.example.cs360weighttracker.data.WeightEntry;

import java.util.ArrayList;
import java.util.Locale;

public class WeightAdapter extends RecyclerView.Adapter<WeightAdapter.WeightViewHolder> {

    public interface OnDeleteListener {
        void onDelete(int weightId);
    }

    public interface OnEditListener {
        void onEdit(WeightEntry entry);
    }

    private final ArrayList<WeightEntry> entries;
    private final OnDeleteListener deleteListener;
    private final OnEditListener editListener;

    public WeightAdapter(ArrayList<WeightEntry> entries, OnDeleteListener deleteListener, OnEditListener editListener) {
        this.entries = entries;
        this.deleteListener = deleteListener;
        this.editListener = editListener;
    }

    @NonNull
    @Override
    public WeightViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_weight_entry, parent, false);
        return new WeightViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull WeightViewHolder holder, int position) {
        WeightEntry entry = entries.get(position);
        holder.tvDate.setText(entry.date);
        holder.tvWeight.setText(String.format(Locale.US,"%.1f lbs", entry.weight));

        holder.btnDelete.setOnClickListener(v -> {
            if (deleteListener != null) deleteListener.onDelete(entry.id);
        });

        holder.itemView.setOnClickListener(v -> {
            if (editListener != null) editListener.onEdit(entry);
        });
    }

    @Override
    public int getItemCount() {
        return entries.size();
    }

    public static class WeightViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate, tvWeight;
        ImageButton btnDelete;

        public WeightViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvWeight = itemView.findViewById(R.id.tvWeight);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}

