package com.example.do4e.ui.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.do4e.R;
import com.example.do4e.db.MedEntity;

import java.util.ArrayList;
import java.util.List;

public class HomeScheduleAdapter extends RecyclerView.Adapter<HomeScheduleAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(MedEntity med);
    }

    private List<MedEntity> meds = new ArrayList<>();
    private String currentTime; // To determine which one is "NOW"
    private int selectedMedId = -1;
    private OnItemClickListener listener;

    public HomeScheduleAdapter(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setMeds(List<MedEntity> meds) {
        this.meds = meds;
        notifyDataSetChanged();
    }

    public void setCurrentTime(String currentTime) {
        this.currentTime = currentTime;
        notifyDataSetChanged();
    }

    public void setSelectedMedId(int id) {
        this.selectedMedId = id;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_home_schedule, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MedEntity med = meds.get(position);
        holder.tvSchedName.setText(med.name);
        holder.tvSchedTime.setText(med.time);
        holder.tvSchedDose.setText(med.dosage);

        // Icon based on type
        if ("Syrup".equalsIgnoreCase(med.medType)) {
            holder.ivMedIcon.setImageResource(R.drawable.serup_24dp_icon);
        } else if ("Syringe".equalsIgnoreCase(med.medType)) {
            holder.ivMedIcon.setImageResource(R.drawable.syringe_24dp_icon);
        } else {
            holder.ivMedIcon.setImageResource(R.drawable.pill_24dp_icon);
        }

        // Check if this is the "NOW" med
        boolean isNow = med.time != null && med.time.equals(currentTime);
        boolean isSelected = med.id_meds == selectedMedId;

        // Visual selection priority: Selected > NOW > Normal
        if (isSelected || (isNow && selectedMedId == -1)) {
            holder.itemView.setBackgroundResource(R.drawable.bg_schedule_card_active);
            holder.vIconBg.setBackgroundTintList(
                    ContextCompat.getColorStateList(holder.itemView.getContext(), R.color.junglegreen));
            holder.ivMedIcon
                    .setImageTintList(ContextCompat.getColorStateList(holder.itemView.getContext(), R.color.white));
            holder.tvSchedTime.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.junglegreen));
            // Only show NOW badge if it's actually now
            holder.tvNowBadge.setVisibility(isNow ? View.VISIBLE : View.GONE);
        } else {
            holder.itemView.setBackgroundResource(R.drawable.bg_schedule_card_normal);
            holder.vIconBg.setBackgroundTintList(
                    ContextCompat.getColorStateList(holder.itemView.getContext(), R.color.MintBg));
            holder.ivMedIcon.setImageTintList(
                    ContextCompat.getColorStateList(holder.itemView.getContext(), R.color.junglegreen));
            holder.tvSchedTime.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.StateGray));
            holder.tvNowBadge.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(med);
            }
        });

        // Status icon (if taken) - logic can be refined later if there's a "taken" flag
        // For now, if daysTaken > 0 and it's daily, we might show it
        // holder.ivStatusIcon.setVisibility(View.VISIBLE / GONE);
    }

    @Override
    public int getItemCount() {
        return meds.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvSchedName, tvSchedTime, tvSchedDose, tvNowBadge;
        ImageView ivMedIcon, ivStatusIcon;
        View vIconBg;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSchedName = itemView.findViewById(R.id.tvSchedName);
            tvSchedTime = itemView.findViewById(R.id.tvSchedTime);
            tvSchedDose = itemView.findViewById(R.id.tvSchedDose);
            tvNowBadge = itemView.findViewById(R.id.tvNowBadge);
            ivMedIcon = itemView.findViewById(R.id.ivMedIcon);
            ivStatusIcon = itemView.findViewById(R.id.ivStatusIcon);
            vIconBg = itemView.findViewById(R.id.vIconBg);
        }
    }
}
