package com.example.voidchat;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ProfileActivity extends AppCompatActivity {
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        auth = FirebaseAuth.getInstance();

        loadProfileData();
        setupLogoutButton();
    }
    private void loadProfileData() {
        TextView textProfileName = findViewById(R.id.textProfileName);
        TextView textProfileEmail = findViewById(R.id.textProfileEmail);

        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            textProfileName.setText("Name: " + user.getDisplayName());
            textProfileEmail.setText("Email: " + user.getEmail());
        }
    }
    private void setupLogoutButton() {
        Button btnLogout = findViewById(R.id.btnLogout);
        btnLogout.setOnClickListener(v -> signOut());
    }
    private void signOut() {
        // Cierra la sesión de Firebase
        auth.signOut();

        // Cierra la sesión de Google para permitir volver a elegir una cuenta
        if (MainActivity.googleClient != null) {
            MainActivity.googleClient.signOut();
        }

        // Redirige a la pantalla de Login y limpia el historial de actividades
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
