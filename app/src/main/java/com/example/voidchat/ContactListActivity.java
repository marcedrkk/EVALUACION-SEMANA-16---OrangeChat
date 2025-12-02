package com.example.voidchat;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.voidchat.adapter.ContactAdapter;
import com.example.voidchat.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.List;

public class ContactListActivity extends AppCompatActivity {
    private ContactAdapter adapter;
    private DatabaseReference usersRef;
    private ValueEventListener usersListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_list);

        usersRef = FirebaseDatabase.getInstance().getReference("users");

        setupProfileButton();
        setupRecyclerView();
    }
    @Override
    protected void onStart() {
        super.onStart();
        // Inicia la escucha de datos cuando la actividad se vuelve visible
        loadContacts();
    }
    @Override
    protected void onStop() {
        super.onStop();
        // Detiene la escucha para ahorrar recursos cuando la actividad no está visible
        if (usersListener != null) {
            usersRef.removeEventListener(usersListener);
        }
    }
    private void setupProfileButton() {
        Button btnGoToProfile = findViewById(R.id.btnGoToProfile);
        btnGoToProfile.setOnClickListener(v ->
                startActivity(new Intent(this, ProfileActivity.class))
        );
    }
    private void setupRecyclerView() {
        RecyclerView recyclerContacts = findViewById(R.id.recyclerContacts);
        adapter = new ContactAdapter(); // ListAdapter ya no necesita el contexto en el constructor
        recyclerContacts.setLayoutManager(new LinearLayoutManager(this));
        recyclerContacts.setAdapter(adapter);
    }
    private void loadContacts() {
        String currentUserId = FirebaseAuth.getInstance().getUid();
        if (currentUserId == null) return;

        // El listener se activa cada vez que hay un cambio en el nodo 'users'
        usersListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<User> contactList = new ArrayList<>();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    User user = ds.getValue(User.class);
                    // Excluye al usuario actual de la lista de contactos
                    if (user != null && !user.getUid().equals(currentUserId)) {
                        contactList.add(user);
                    }
                }
                // submitList se encarga de las animaciones y actualizaciones de forma eficiente
                adapter.submitList(contactList);

                if (contactList.isEmpty()) {
                    Toast.makeText(ContactListActivity.this, "No other users found", Toast.LENGTH_LONG).show();
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ContactListActivity.this, "Failed to load contacts: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        };
        usersRef.addValueEventListener(usersListener);
    }
}
