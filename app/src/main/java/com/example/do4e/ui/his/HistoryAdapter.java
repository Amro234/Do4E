package com.example.do4e.ui.his;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.do4e.R;
import com.example.do4e.db.MedEntity;

import java.util.List;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

    private List<MedEntity> medList;

    public HistoryAdapter(List<MedEntity> medList) {
        this.medList = medList;
    }

    public void updateList(List<MedEntity> newList) {
        this.medList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MedEntity med = medList.get(position);
        holder.tvName.setText(med.name);
        holder.tvTime.setText(med.time);
    }

    @Override
    public int getItemCount() {
        return medList == null ? 0 : medList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvTime;

        ViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_history_name);
            tvTime = itemView.findViewById(R.id.tv_history_time);
        }
    }
}
