package com.emsi.emsipresence;

import android.widget.EditText;
import android.widget.RadioButton;

public class Etudiant {
    private String id;
    private String nom;
    private String prenom;
    private transient RadioButton rbPresent;
    private transient RadioButton rbAbsent;
    private transient EditText etRemarque;
    private String remarque;
    private boolean present;

    public String getRemarque() {
        return remarque;
    }

    public void setRemarque(String remarque) {
        this.remarque = remarque;
    }

    public boolean isPresent() {
        return present;
    }

    public void setPresent(boolean present) {
        this.present = present;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getPrenom() {
        return prenom;
    }

    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }

    public RadioButton getRbPresent() {
        return rbPresent;
    }

    public void setRbPresent(RadioButton rbPresent) {
        this.rbPresent = rbPresent;
    }

    public RadioButton getRbAbsent() {
        return rbAbsent;
    }

    public void setRbAbsent(RadioButton rbAbsent) {
        this.rbAbsent = rbAbsent;
    }

    public EditText getEtRemarque() {
        return etRemarque;
    }

    public void setEtRemarque(EditText etRemarque) {
        this.etRemarque = etRemarque;
    }

    public Etudiant(String id, String nom, String prenom, RadioButton rbPresent, RadioButton rbAbsent, EditText etRemarque) {
        this.id = id;
        this.nom = nom;
        this.prenom = prenom;
        this.rbPresent = rbPresent;
        this.rbAbsent = rbAbsent;
        this.etRemarque = etRemarque;
    }

}

