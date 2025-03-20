package com.example.homediginat;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class CardAdapter extends RecyclerView.Adapter<CardAdapter.ViewHolder> {

    private Context context;
    private List<CardModel> cardList;
    private DeleteCallback deleteCallback;
    private OnItemClickListener onItemClickListener; // 👈 Added item click listener

    public CardAdapter(Context context, ArrayList<CardModel> cardList, DeleteCallback deleteCallback, OnItemClickListener onItemClickListener) {
        this.context = context;
        this.cardList = cardList;
        this.deleteCallback = deleteCallback;
        this.onItemClickListener = onItemClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(context).inflate(R.layout.card_item, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CardModel model = cardList.get(position);

        holder.textView.setText(model.getName());
        holder.imageView.setImageResource(model.getImage());

        // 🔥 Set up the delete button
        holder.deleteButton.setOnClickListener(v -> {
            new AlertDialog.Builder(context)
                    .setTitle("Delete Folder Card")
                    .setMessage("Are you sure you want to delete this folder card?")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        int currentPosition = holder.getAdapterPosition();
                        if (currentPosition != RecyclerView.NO_POSITION) {
                            cardList.remove(currentPosition);
                            notifyItemRemoved(currentPosition);
                            Toast.makeText(context, "Folder deleted", Toast.LENGTH_SHORT).show();

                            if (deleteCallback != null) {
                                deleteCallback.onDelete();
                            }
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        // Set up item click listener on the whole card
        holder.itemView.setOnClickListener(v -> {
            int currentPosition = holder.getAdapterPosition();
            if (currentPosition != RecyclerView.NO_POSITION && onItemClickListener != null) {
                CardModel selectedCard = cardList.get(currentPosition);
                onItemClickListener.onItemClick(selectedCard);
            }
        });
    }

    @Override
    public int getItemCount() {
        return cardList.size();
    }
    public void addCard(CardModel card) {
        cardList.add(card);
        notifyItemInserted(cardList.size() - 1);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textView;
        ImageView imageView;
        ImageButton deleteButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.cardText);
            imageView = itemView.findViewById(R.id.cardImage);
            deleteButton = itemView.findViewById(R.id.deleteButton);
        }
    }

    // Callback interface for delete events
    public interface DeleteCallback {
        void onDelete();
    }

    // item click events
    public interface OnItemClickListener {
        void onItemClick(CardModel card);
    }
}
