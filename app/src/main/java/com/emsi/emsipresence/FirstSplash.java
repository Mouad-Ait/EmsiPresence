package com.emsi.emsipresence;


import static androidx.core.content.ContextCompat.startActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;


import androidx.appcompat.app.AppCompatActivity;




public class FirstSplash extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_first_splash);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // Une fois le délai écoulé, on redirige vers la page d'accueil (MainActivity)
                Intent intent = new Intent(FirstSplash.this, SignInActivity.class);
                startActivity(intent);
                finish(); // Ferme l'écran Splash une fois l'activité principale lancée
            }
        }, 2000); // 2000 millisecondes = 2 secondes
    }
}