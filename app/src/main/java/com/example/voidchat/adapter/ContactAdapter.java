package com.example.voidchat.adapter;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.example.voidchat.ChatActivity;
import com.example.voidchat.R;
import com.example.voidchat.model.User;

// Usamos ListAdapter para un manejo de listas más eficiente y con animaciones automáticas
public class ContactAdapter extends ListAdapter<User, ContactAdapter.ContactViewHolder> {

    public ContactAdapter() {
        super(USER_DIFF_CALLBACK);
    }

    @NonNull
    @Override
    public ContactViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_contact, parent, false);
        return new ContactViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ContactViewHolder holder, int position) {
        User user = getItem(position);
        holder.bind(user);
    }

    // Clase interna estática para el ViewHolder, una buena práctica para evitar fugas de memoria
    static class ContactViewHolder extends RecyclerView.ViewHolder {
        private final TextView textInitial;
        private final TextView textName;
        private final TextView textEmail;

        public ContactViewHolder(@NonNull View itemView) {
            super(itemView);
            textInitial = itemView.findViewById(R.id.textInitial);
            textName = itemView.findViewById(R.id.textName);
            textEmail = itemView.findViewById(R.id.textEmail);
        }
        // Método para "enlazar" los datos del usuario con las vistas del item
        public void bind(final User user) {
            textName.setText(user.getDisplayName());
            textEmail.setText(user.getEmail());

            if (user.getDisplayName() != null && !user.getDisplayName().isEmpty()) {
                textInitial.setText(user.getDisplayName().substring(0, 1).toUpperCase());
            }

            // Listener para abrir el chat al hacer clic en un contacto
            itemView.setOnClickListener(v -> {
                Context context = itemView.getContext();
                Intent intent = new Intent(context, ChatActivity.class);
                intent.putExtra("contactUid", user.getUid());
                intent.putExtra("contactName", user.getDisplayName());
                context.startActivity(intent);
            });
        }
    }

    // Objeto DiffUtil para que ListAdapter sepa qué ha cambiado y cómo animar la lista
    private static final DiffUtil.ItemCallback<User> USER_DIFF_CALLBACK = new DiffUtil.ItemCallback<User>() {
        @Override
        public boolean areItemsTheSame(@NonNull User oldItem, @NonNull User newItem) {
            // Los items son los mismos si sus IDs son iguales
            return oldItem.getUid().equals(newItem.getUid());
        }
        @Override
        public boolean areContentsTheSame(@NonNull User oldItem, @NonNull User newItem) {
            // El contenido es el mismo si los campos relevantes no han cambiado
            return oldItem.getDisplayName().equals(newItem.getDisplayName()) && oldItem.getEmail().equals(newItem.getEmail());
        }
    };
}
