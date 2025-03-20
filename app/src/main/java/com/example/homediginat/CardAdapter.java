package com.example.homediginat;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class CardAdapter extends ArrayAdapter<CardModel> {

    private Context context;
    private ArrayList<CardModel> cardList;
    private DeleteCallback deleteCallback; // Callback to notify Activity on deletion

    // Constructor with delete callback
    public CardAdapter(Context context, ArrayList<CardModel> list, DeleteCallback deleteCallback) {
        super(context, 0, list);
        this.context = context;
        this.cardList = list;
        this.deleteCallback = deleteCallback;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View itemView = convertView;
        if (itemView == null) {
            itemView = LayoutInflater.from(getContext()).inflate(R.layout.card_item, parent, false);
        }

        CardModel model = getItem(position);

        TextView textView = itemView.findViewById(R.id.cardText);
        ImageView imageView = itemView.findViewById(R.id.cardImage);
        ImageButton deleteButton = itemView.findViewById(R.id.deleteButton);

        if (model != null) {
            textView.setText(model.getName());
            imageView.setImageResource(model.getImage());

            deleteButton.setOnClickListener(v -> {
                // Show a confirmation dialog before deletion
                new AlertDialog.Builder(context)
                        .setTitle("Delete Folder Card")
                        .setMessage("Are you sure you want to delete this folder card?")
                        .setPositiveButton("Delete", (dialog, which) -> {
                            cardList.remove(position);     // Remove from list
                            notifyDataSetChanged();        // Refresh GridView
                            Toast.makeText(context, "Folder deleted", Toast.LENGTH_SHORT).show();

                            if (deleteCallback != null) {
                                deleteCallback.onDelete(); // Notify activity to save the list
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        }

        return itemView;
    }

    // Interface for notifying Activity to perform actions (like save)
    public interface DeleteCallback {
        void onDelete();
    }
}
