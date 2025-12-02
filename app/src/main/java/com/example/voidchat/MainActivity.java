// RUTA: com.example.voidchat/MainActivity.java
package com.example.voidchat;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.example.voidchat.model.User;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.FirebaseDatabase;

public class MainActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 100;
    private static final String TAG_GOOGLE_ERROR = "GOOGLE_SIGN_IN_ERROR";
    private static final String TAG_FIREBASE_ERROR = "FIREBASE_ERROR";

    // Se hace estático para poder cerrar sesión desde ProfileActivity
    public static GoogleSignInClient googleClient;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        auth = FirebaseAuth.getInstance();
        // Si el usuario ya está logueado, se salta esta pantalla
        if (auth.getCurrentUser() != null) {
            navigateToContactList();
            return;
        }

        setContentView(R.layout.activity_main);
        setupGoogleSignIn();
        setupSignInButton();
    }

    private void navigateToContactList() {
        startActivity(new Intent(this, ContactListActivity.class));
        finish(); // Cierra esta actividad para que no se pueda volver atrás
    }

    private void setupGoogleSignIn() {
        // Configura Google Sign-In para solicitar el ID Token necesario para Firebase
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleClient = GoogleSignIn.getClient(this, gso);
    }

    private void setupSignInButton() {
        Button googleButton = findViewById(R.id.btnGoogle);
        googleButton.setOnClickListener(v -> {
            Intent intent = googleClient.getSignInIntent();
            startActivityForResult(intent, RC_SIGN_IN);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            try {
                // El resultado del intent de Google Sign-In
                GoogleSignInAccount account = GoogleSignIn.getSignedInAccountFromIntent(data).getResult(ApiException.class);
                if (account != null) {
                    firebaseAuthWithGoogle(account.getIdToken());
                }
            } catch (ApiException e) {
                Log.e(TAG_GOOGLE_ERROR, "Google sign in failed", e);
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        auth.signInWithCredential(credential)
                .addOnSuccessListener(authResult -> {
                    // Si la autenticación con Firebase es exitosa, guardamos/actualizamos el usuario y continuamos
                    saveUserToDatabase();
                    navigateToContactList();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Firebase Authentication Failed.", Toast.LENGTH_SHORT).show();
                    Log.e(TAG_FIREBASE_ERROR, "Firebase auth failed", e);
                });
    }

    private void saveUserToDatabase() {
        FirebaseUser firebaseUser = auth.getCurrentUser();
        if (firebaseUser == null) return;

        // Crea un objeto User con los datos del perfil de Google
        String uid = firebaseUser.getUid();
        String email = firebaseUser.getEmail();
        String displayName = firebaseUser.getDisplayName();
        String photoUrl = (firebaseUser.getPhotoUrl() != null) ? firebaseUser.getPhotoUrl().toString() : "";

        User user = new User(uid, email, displayName, photoUrl);

        // Guarda el objeto User en Realtime Database, usando el UID como clave
        FirebaseDatabase.getInstance().getReference("users").child(uid)
                .setValue(user)
                .addOnFailureListener(e -> Log.e(TAG_FIREBASE_ERROR, "Failed to save user to database", e));
    }
}
