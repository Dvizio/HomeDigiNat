package com.example.homediginat;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class CardAdapter extends ArrayAdapter<CardModel> {

    public CardAdapter(Context context, ArrayList<CardModel> list) {
        super(context, 0, list);
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

        if (model != null) {
            textView.setText(model.getName());
            imageView.setImageResource(model.getImage());
        }

        return itemView;
    }
}
