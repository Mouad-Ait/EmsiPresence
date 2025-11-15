package com.emsi.emsipresence;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class StudentAdapter extends RecyclerView.Adapter<StudentAdapter.ViewHolder> {

    private List<Etudiant> studentList;

    public StudentAdapter(List<Etudiant> studentList) {
        this.studentList = studentList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_student, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Etudiant student = studentList.get(position);
        holder.nameTextView.setText(student.getNom());
        holder.remarqueTextView.setText(student.getRemarque());  // Assurez-vous que getRemarque() existe dans Etudiant

        // Mettre à jour l'état des boutons en fonction de la présence de l'étudiant
        if (student.isPresent()) {
            holder.btnPresent.setEnabled(false);  // Désactiver le bouton "Présent" si déjà marqué comme présent
            holder.btnAbsent.setEnabled(true);   // Activer le bouton "Absent" si présent
        } else {
            holder.btnPresent.setEnabled(true);   // Activer le bouton "Présent" si absent
            holder.btnAbsent.setEnabled(false);  // Désactiver le bouton "Absent" si déjà marqué comme absent
        }

        // Click listener pour "Présent"
        holder.btnPresent.setOnClickListener(v -> {
            student.setPresent(true);  // Marquer comme présent
            notifyItemChanged(position);  // Met à jour l'élément dans la liste
        });

        // Click listener pour "Absent"
        holder.btnAbsent.setOnClickListener(v -> {
            student.setPresent(false);  // Marquer comme absent
            notifyItemChanged(position);  // Met à jour l'élément dans la liste
        });
    }

    @Override
    public int getItemCount() {
        return studentList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView nameTextView;
        TextView remarqueTextView;
        Button btnPresent;
        Button btnAbsent;

        public ViewHolder(View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.tvName);
            remarqueTextView = itemView.findViewById(R.id.tvRemarque);
            btnPresent = itemView.findViewById(R.id.btnPresent);
            btnAbsent = itemView.findViewById(R.id.btnAbsent);
        }
    }
}
