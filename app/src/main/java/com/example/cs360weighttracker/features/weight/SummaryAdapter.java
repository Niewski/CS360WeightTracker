package com.example.cs360weighttracker.features.weight;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cs360weighttracker.R;
import com.example.cs360weighttracker.data.TimePeriodAverage;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * RecyclerView adapter that displays {@link TimePeriodAverage} rows
 * (weekly or monthly summaries) as cards showing the period label,
 * average weight, and entry count.
 */
public class SummaryAdapter extends RecyclerView.Adapter<SummaryAdapter.SummaryViewHolder> {

    private final ArrayList<TimePeriodAverage> data = new ArrayList<>();

    /**
     * Replaces the current data set and refreshes the list.
     */
    public void setData(List<TimePeriodAverage> newData) {
        data.clear();
        if (newData != null) data.addAll(newData);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SummaryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_summary_entry, parent, false);
        return new SummaryViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull SummaryViewHolder holder, int position) {
        TimePeriodAverage item = data.get(position);
        holder.tvPeriod.setText(item.period);
        holder.tvAvgWeight.setText(String.format(Locale.US, "Avg: %.1f lbs", item.average));
        holder.tvEntryCount.setText(String.format(Locale.US, "%d entries", item.entryCount));
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    /**
     * ViewHolder for a single summary row.
     */
    public static class SummaryViewHolder extends RecyclerView.ViewHolder {
        TextView tvPeriod, tvAvgWeight, tvEntryCount;

        public SummaryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPeriod = itemView.findViewById(R.id.tvPeriod);
            tvAvgWeight = itemView.findViewById(R.id.tvAvgWeight);
            tvEntryCount = itemView.findViewById(R.id.tvEntryCount);
        }
    }
}
