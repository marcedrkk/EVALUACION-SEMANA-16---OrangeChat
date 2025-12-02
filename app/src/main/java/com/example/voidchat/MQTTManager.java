package com.example.voidchat;
import androidx.annotation.NonNull;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MQTTManager {

    // --- Credenciales y Constantes ---
    private static final String BROKER_URL = "ssl://2b34e36ea592466995f32ddf7ba42f12.s1.eu.hivemq.cloud:8883";
    private static final String USERNAME = "marce";
    private static final String PASSWORD = "12345Marce";
    // --- Variables de Instancia ---
    @NonNull private final MQTTListener listener;
    // @Volatile asegura que los cambios en esta variable sean visibles para todos los hilos inmediatamente.
    // Es crucial porque el cliente se crea y destruye en un hilo de fondo.
    private volatile MqttClient client;
    // Usamos un ExecutorService con un solo hilo para poner todas las operaciones de red en una cola.
    // Esto es más eficiente que crear un 'new Thread()' cada vez.
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    // --- Interfaz de Callback ---
    public interface MQTTListener {
        void onConnected();
        void onDisconnected();
        void onMessage(String topic, String msg, String senderUid);
        void onError(String error);
    }
    public MQTTManager(@NonNull MQTTListener listener) {
        this.listener = listener;
    }
    // --- Métodos Públicos ---
    public void connectAndSubscribe(final String topic) {
        executorService.submit(() -> {
            try {
                // Si el cliente ya existe y está conectado, solo nos suscribimos si es necesario y salimos.
                if (client != null && client.isConnected()) {
                    client.subscribe(topic, 1); // QoS 1: At least once
                    return;
                }

                // Generamos un ID de cliente único para esta sesión.
                String clientId = "AndroidClient-" + UUID.randomUUID().toString();
                client = new MqttClient(BROKER_URL, clientId, null);

                // Asignamos el callback ANTES de conectar para no perder ningún mensaje inicial.
                client.setCallback(getMqttCallback());

                // Conectamos con las credenciales.
                client.connect(createConnectOptions());
                listener.onConnected();

                // Nos suscribimos al tópico deseado después de una conexión exitosa.
                client.subscribe(topic, 1);

            } catch (Exception e) {
                e.printStackTrace();
                listener.onError("Connection or Subscription Failed: " + e.getMessage());
            }
        });
    }

    /**
     * Publica un mensaje en un tópico específico. La operación se ejecuta en un hilo de fondo.
     * @param topic El tópico en el que publicar.
     * @param text El texto del mensaje.
     * @param userId El UID del usuario que envía el mensaje.
     */
    public void sendMessage(final String topic, final String text, final String userId) {
        executorService.submit(() -> {
            // Solo publicamos si el cliente existe y está conectado.
            if (client == null || !client.isConnected()) {
                listener.onError("Cannot send message: Not connected.");
                return;
            }

            try {
                JSONObject payload = new JSONObject();
                payload.put("text", text);
                payload.put("userId", userId);

                MqttMessage message = new MqttMessage(payload.toString().getBytes());
                message.setQos(1); // Calidad de Servicio 1: "entregar al menos una vez".
                client.publish(topic, message);

            } catch (JSONException e) {
                listener.onError("JSON Error: " + e.getMessage());
            } catch (Exception e) {
                listener.onError("Send Message Failed: " + e.getMessage());
            }
        });
    }

    /**
     * Cierra la conexión con el broker MQTT de forma segura, La operación se ejecuta en un hilo de fondo.
     */
    public void disconnect() {
        executorService.submit(() -> {
            if (client == null || !client.isConnected()) {
                // Si ya está desconectado, no hacemos nada.
                return;
            }
            try {
                // Desuscribirse de todos los tópicos y desconectar.
                client.disconnect();
            } catch (Exception e) {
                listener.onError("Disconnection Error: " + e.getMessage());
            } finally {
                // 'finally' asegura que estas líneas se ejecuten incluso si hay un error.
                client = null;
                listener.onDisconnected();
            }
        });
    }


    // --- Métodos Privados de Utilidad ---

    /**
     * Crea y configura las opciones de conexión.
     * @return Un objeto MqttConnectOptions configurado.
     */
    private MqttConnectOptions createConnectOptions() {
        MqttConnectOptions options = new MqttConnectOptions();
        options.setUserName(USERNAME);
        options.setPassword(PASSWORD.toCharArray());
        options.setAutomaticReconnect(true); // Intenta reconectar automáticamente si la conexión se pierde.
        options.setCleanSession(true);       // No guarda el estado de la sesión al desconectar.
        return options;
    }
    private MqttCallback getMqttCallback() {
        return new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                // Se llama si la conexión se pierde inesperadamente.
                listener.onError("Connection Lost");
                listener.onDisconnected();
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) {
                // Se llama cuando llega un nuevo mensaje.
                try {
                    String payload = new String(message.getPayload());
                    JSONObject obj = new JSONObject(payload);

                    String text = obj.getString("text");
                    String senderUid = obj.getString("userId");

                    listener.onMessage(topic, text, senderUid);

                } catch (Exception e) {
                    listener.onError("Error parsing message: " + e.getMessage());
                }
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                // Se llama cuando un mensaje publicado (con QoS > 0) ha sido entregado.
                // No necesitamos hacer nada aquí por ahora.
            }
        };
    }
}

