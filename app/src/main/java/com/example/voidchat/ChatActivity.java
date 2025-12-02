// RUTA: com.example.voidchat/ChatActivity.java
package com.example.voidchat;

import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.voidchat.adapter.MessageAdapter;
import com.example.voidchat.model.Message;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class ChatActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private EditText messageInput;
    private ImageButton sendButton;
    private MessageAdapter messageAdapter;

    // --- Firebase & MQTT ---
    private FirebaseAuth auth;
    private DatabaseReference messagesRef;
    private DatabaseReference typingStatusRef;
    private ValueEventListener messagesListener;
    private ValueEventListener typingListener;
    private MQTTManager mqttManager;

    // --- Datos del Chat ---
    private String currentUserId;
    private String contactId;
    private String chatRoomId;
    private String mqttTopic;

    // --- Lógica para "Está escribiendo..." ---
    private final Handler typingHandler = new Handler();
    private Runnable typingTimeoutRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // Paso 1: Validar y obtener los datos necesarios para iniciar el chat
        if (!setupIntentData()) {
            // Si no hay datos válidos (ej. un UID nulo), cerramos la actividad.
            finish();
            return;
        }

        // Paso 2: Configurar todos los componentes
        setupViews();
        setupFirebase();
        setupRecyclerView();
        setupClickListeners();
        setupMqtt();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Empezamos a escuchar cambios en la base de datos cuando la pantalla es visible
        attachFirebaseListeners();
    }
    @Override
    protected void onStop() {
        super.onStop();
        // Dejamos de escuchar para ahorrar recursos cuando la pantalla no es visible
        detachFirebaseListeners();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Limpieza final para evitar fugas de memoria
        if (currentUserId != null && typingStatusRef != null) {
            typingStatusRef.child(currentUserId).setValue(false);
        }
        if (mqttManager != null) {
            mqttManager.disconnect();
        }
    }

    // --- MÉTODOS DE CONFIGURACIÓN (Setup) ---
    private boolean setupIntentData() {
        auth = FirebaseAuth.getInstance();
        currentUserId = auth.getUid();
        contactId = getIntent().getStringExtra("contactUid");
        String contactName = getIntent().getStringExtra("contactName");

        if (currentUserId == null || contactId == null) {
            Toast.makeText(this, "Error: Chat data is missing.", Toast.LENGTH_LONG).show();
            return false;
        }
        // Generamos un ID de sala de chat consistente para ambos usuarios
        chatRoomId = currentUserId.compareTo(contactId) < 0
                ? currentUserId + "_" + contactId
                : contactId + "_" + currentUserId;
        mqttTopic = "chat/" + chatRoomId;

        // Configura el título de la barra de herramientas
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar_chat);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(contactName);
        }
        return true;
    }
    private void setupViews() {
        recyclerView = findViewById(R.id.recyclerChat);
        messageInput = findViewById(R.id.editMsg);
        sendButton = findViewById(R.id.btnSend);
    }
    private void setupFirebase() {
        DatabaseReference chatRootRef = FirebaseDatabase.getInstance().getReference("chats").child(chatRoomId);
        messagesRef = chatRootRef.child("messages");
        typingStatusRef = chatRootRef.child("typing_status");
    }
    private void setupRecyclerView() {
        messageAdapter = new MessageAdapter();
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true); // Los mensajes nuevos aparecen abajo
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(messageAdapter);
    }
    private void setupClickListeners() {
        sendButton.setOnClickListener(v -> sendMessage());
        setupTypingDetector();
    }
    private void setupMqtt() {
        // Los botones de conexión de MQTT están ocultos, pero la lógica se puede mantener si se necesitan
        Button btnMqttConnect = findViewById(R.id.btnMqttConnect);
        Button btnMqttDisconnect = findViewById(R.id.btnMqttDisconnect);

        mqttManager = new MQTTManager(new MQTTManager.MQTTListener() {
            @Override public void onConnected() {
                runOnUiThread(() -> Toast.makeText(ChatActivity.this, "MQTT Conectado", Toast.LENGTH_SHORT).show());
            }
            @Override public void onDisconnected() {
                runOnUiThread(() -> Toast.makeText(ChatActivity.this, "MQTT Desconectado", Toast.LENGTH_SHORT).show());
            }
            @Override public void onMessage(String topic, String msg, String senderUid) {
                // Solo reacciona si el mensaje es para este chat y no es mío
                if (topic.equals(mqttTopic) && !senderUid.equals(currentUserId)) {
                    // Podríamos mostrar una notificación aquí en lugar de un Toast
                }
            }
            @Override public void onError(String error) {
                runOnUiThread(() -> Toast.makeText(ChatActivity.this, "Error MQTT: " + error, Toast.LENGTH_SHORT).show());
            }
        });
        // Conectar automáticamente al entrar al chat
        mqttManager.connectAndSubscribe(mqttTopic);

        btnMqttConnect.setOnClickListener(v -> mqttManager.connectAndSubscribe(mqttTopic));
        btnMqttDisconnect.setOnClickListener(v -> mqttManager.disconnect());
    }
    // --- LÓGICA PRINCIPAL DEL CHAT ---

    private void sendMessage() {
        String text = messageInput.getText().toString().trim();
        if (text.isEmpty()) {
            return;
        }

        FirebaseUser currentUser = auth.getCurrentUser();
        String displayName = (currentUser != null && currentUser.getDisplayName() != null) ? currentUser.getDisplayName() : "Yo";

        Message message = new Message(currentUserId, displayName, text, System.currentTimeMillis());
        // Envía el mensaje a Firebase Realtime Database
        messagesRef.push().setValue(message);
        // Opcional: envía una notificación a través de MQTT
        mqttManager.sendMessage(mqttTopic, text, currentUserId);
        messageInput.setText("");
        typingStatusRef.child(currentUserId).setValue(false); // Deja de mostrar "está escribiendo"
    }
    private void attachFirebaseListeners() {
        // Listener para leer los mensajes
        messagesListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Message> messageList = new ArrayList<>();
                long lastDate = -1;
                for (DataSnapshot s : snapshot.getChildren()) {
                    Message message = s.getValue(Message.class);
                    if (message == null) continue;

                    // Agrega un separador de fecha si el día del mensaje es diferente al anterior
                    long messageDay = getDayStart(message.getTime());
                    if (messageDay != lastDate) {
                        lastDate = messageDay;
                        messageList.add(Message.createSeparator(getLabelForDate(message.getTime())));
                    }
                    messageList.add(message);
                }
                // ListAdapter se encarga de las animaciones y la actualización
                messageAdapter.submitList(messageList);
                if (!messageList.isEmpty()) {
                    recyclerView.scrollToPosition(messageList.size() - 1);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ChatActivity.this, "Failed to load messages.", Toast.LENGTH_SHORT).show();
            }
        };
        messagesRef.addValueEventListener(messagesListener);
        // Listener para saber si el otro usuario está escribiendo
        typingListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean isTyping = snapshot.getValue(Boolean.class);
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setSubtitle(Boolean.TRUE.equals(isTyping) ? "Está escribiendo..." : null);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        };
        typingStatusRef.child(contactId).addValueEventListener(typingListener);
    }
    private void detachFirebaseListeners() {
        if (messagesListener != null) {
            messagesRef.removeEventListener(messagesListener);
        }
        if (typingListener != null) {
            typingStatusRef.child(contactId).removeEventListener(typingListener);
        }
    }
    private void setupTypingDetector() {
        typingTimeoutRunnable = () -> typingStatusRef.child(currentUserId).setValue(false);

        messageInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                typingHandler.removeCallbacks(typingTimeoutRunnable); // Reinicia el temporizador
                if (s.length() > 0) {
                    typingStatusRef.child(currentUserId).setValue(true);
                    typingHandler.postDelayed(typingTimeoutRunnable, 1500); // Se marcará como "no escribiendo" tras 1.5s de inactividad
                } else {
                    typingStatusRef.child(currentUserId).setValue(false);
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }
    // --- MÉTODOS DE UTILIDAD PARA FECHAS ---
    private long getDayStart(long time) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(time);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }
    private String getLabelForDate(long time) {
        Calendar today = Calendar.getInstance();
        Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.DAY_OF_YEAR, -1);
        Calendar messageDate = Calendar.getInstance();
        messageDate.setTimeInMillis(time);
        if (isSameDay(messageDate, today)) return "Hoy";
        if (isSameDay(messageDate, yesterday)) return "Ayer";
        return new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(time);
    }
    private boolean isSameDay(Calendar c1, Calendar c2) {
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
                c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR);
    }
}
