// RUTA: com.example.voidchat.adapter/MessageAdapter.javapackage com.example.voidchat.adapter;
package com.example.voidchat.adapter;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.example.voidchat.R;
import com.example.voidchat.model.Message;
import com.google.firebase.auth.FirebaseAuth;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Objects; // <-- Importación necesaria para la corrección

public class MessageAdapter extends ListAdapter<Message, RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_MESSAGE = 1;
    private static final int VIEW_TYPE_SEPARATOR = 2;

    private final String currentUserId;

    public MessageAdapter() {
        super(MESSAGE_DIFF_CALLBACK);
        this.currentUserId = FirebaseAuth.getInstance().getUid();
    }

    @Override
    public int getItemViewType(int position) {
        return getItem(position).isSeparator() ? VIEW_TYPE_SEPARATOR : VIEW_TYPE_MESSAGE;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == VIEW_TYPE_SEPARATOR) {
            View view = inflater.inflate(R.layout.item_separator, parent, false);
            return new SeparatorViewHolder(view);
        }
        View view = inflater.inflate(R.layout.item_message, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message message = getItem(position);
        if (holder.getItemViewType() == VIEW_TYPE_SEPARATOR) {
            ((SeparatorViewHolder) holder).bind(message);
        } else {
            ((MessageViewHolder) holder).bind(message, currentUserId);
        }
    }

    // --- VIEW HOLDERS (SIN CAMBIOS) ---

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        private final TextView textMsg, textUser, textHour;
        private final LinearLayout contentLayout;
        MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            textMsg = itemView.findViewById(R.id.textMsg);
            textUser = itemView.findViewById(R.id.textUser);
            textHour = itemView.findViewById(R.id.textHour);
            contentLayout = itemView.findViewById(R.id.message_content_layout);
        }
        void bind(Message message, String currentUserId) {
            textMsg.setText(message.getText());
            textUser.setText(message.getUsername());
            textHour.setText(new SimpleDateFormat("HH:mm", Locale.getDefault()).format(message.getTime()));
            boolean isSentByMe = message.getUserId().equals(currentUserId);
            Context context = itemView.getContext();
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) contentLayout.getLayoutParams();
            if (isSentByMe) {
                params.removeRule(RelativeLayout.ALIGN_PARENT_START);
                params.addRule(RelativeLayout.ALIGN_PARENT_END);
                textMsg.setBackgroundResource(R.drawable.bubble_chatvoid_my);
                textUser.setTextColor(ContextCompat.getColor(context, R.color.cream_primary_variant));
            } else {
                params.removeRule(RelativeLayout.ALIGN_PARENT_END);
                params.addRule(RelativeLayout.ALIGN_PARENT_START);
                textMsg.setBackgroundResource(R.drawable.voidchat_bubble_glow);
                textUser.setTextColor(ContextCompat.getColor(context, R.color.cream_primary));
            }
            contentLayout.setLayoutParams(params);
            Animation anim = AnimationUtils.loadAnimation(context, R.anim.msg_pop);
            itemView.startAnimation(anim);
        }
    }

    static class SeparatorViewHolder extends RecyclerView.ViewHolder {
        private final TextView textSeparator;
        SeparatorViewHolder(@NonNull View itemView) {
            super(itemView);
            textSeparator = (TextView) itemView;
        }
        void bind(Message message) {
            textSeparator.setText(message.getSeparatorLabel());
        }
    }

    // =============================================================
    //              ¡AQUÍ ESTÁ LA CORRECCIÓN!
    // =============================================================
    private static final DiffUtil.ItemCallback<Message> MESSAGE_DIFF_CALLBACK = new DiffUtil.ItemCallback<Message>() {
        @Override
        public boolean areItemsTheSame(@NonNull Message oldItem, @NonNull Message newItem) {
            // Si ambos son separadores, compáralos por su etiqueta.
            if (oldItem.isSeparator() && newItem.isSeparator()) {
                return Objects.equals(oldItem.getSeparatorLabel(), newItem.getSeparatorLabel());
            }
            // Si ambos son mensajes, compáralos por su timestamp (que actúa como ID único).
            if (!oldItem.isSeparator() && !newItem.isSeparator()) {
                return oldItem.getTime() == newItem.getTime();
            }
            // Si uno es separador y el otro no, definitivamente no son el mismo item.
            return false;
        }

        @Override
        public boolean areContentsTheSame(@NonNull Message oldItem, @NonNull Message newItem) {
            // Si ambos son separadores, el contenido es el mismo si las etiquetas son iguales.
            if (oldItem.isSeparator() && newItem.isSeparator()) {
                return Objects.equals(oldItem.getSeparatorLabel(), newItem.getSeparatorLabel());
            }
            // Si ambos son mensajes, el contenido es el mismo si el texto es igual.
            // Usamos Objects.equals para manejar de forma segura si alguno de los textos fuera null.
            if (!oldItem.isSeparator() && !newItem.isSeparator()) {
                return Objects.equals(oldItem.getText(), newItem.getText());
            }
            // Si son de tipos diferentes, su contenido no es el mismo.
            return false;
        }
    };
}

