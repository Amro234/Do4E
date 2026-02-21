package com.example.do4e.ui.his;

import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.do4e.R;
import com.example.do4e.db.AppDataBase;
import com.example.do4e.db.MedEntity;
import com.example.do4e.reminder.ReminderScheduler;

import java.util.Calendar;
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

        Context context = holder.itemView.getContext();

        // DELETE
        holder.btnDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(context)
                    .setTitle("Delete")
                    .setMessage("Delete \"" + med.name + "\"?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        new Thread(() -> {
                            // Cancel the scheduled alarm first
                            ReminderScheduler.cancelAlarm(context, med.name, med.time);
                            AppDataBase.getInstance(context).medDAO().delete(med);
                            medList.remove(position);
                            ((history) context).runOnUiThread(() -> notifyItemRemoved(position));
                        }).start();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        // UPDATE / EDIT
        holder.btnUpdate.setOnClickListener(v -> showEditDialog(context, med, position));
    }

    private void showEditDialog(Context context, MedEntity med, int position) {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_edit_med, null);
        EditText etName = dialogView.findViewById(R.id.et_edit_name);
        TextView tvTime = dialogView.findViewById(R.id.tv_edit_time);
        Button btnPickTime = dialogView.findViewById(R.id.btn_edit_pick_time);

        etName.setText(med.name);
        tvTime.setText(med.time);

        // time picker inside the dialog
        btnPickTime.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            new TimePickerDialog(context, (view, h, m) -> {
                String amPm = h >= 12 ? "PM" : "AM";
                int displayHour = h % 12;
                if (displayHour == 0)
                    displayHour = 12;
                tvTime.setText(String.format(java.util.Locale.getDefault(), "%02d:%02d %s", displayHour, m, amPm));
            }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), false).show();
        });

        new AlertDialog.Builder(context)
                .setTitle("Edit Medicine")
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newName = etName.getText().toString().trim();
                    String newTime = tvTime.getText().toString().trim();
                    if (newName.isEmpty()) {
                        Toast.makeText(context, "Name cannot be empty", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    new Thread(() -> {
                        med.name = newName;
                        med.time = newTime;
                        AppDataBase.getInstance(context).medDAO().update(med);
                        medList.set(position, med);
                        ((history) context).runOnUiThread(() -> notifyItemChanged(position));
                    }).start();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public int getItemCount() {
        return medList == null ? 0 : medList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvTime;
        Button btnDelete, btnUpdate;

        ViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_history_name);
            tvTime = itemView.findViewById(R.id.tv_history_time);
            btnDelete = itemView.findViewById(R.id.btn_delete);
            btnUpdate = itemView.findViewById(R.id.btn_update);
        }
    }
}
