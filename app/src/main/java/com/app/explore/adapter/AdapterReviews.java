package com.app.explore.adapter;

import android.content.Context;
import androidx.recyclerview.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.app.explore.R;
import com.google.maps.model.PlaceDetails.Review;

public class AdapterReviews extends RecyclerView.Adapter<AdapterReviews.ViewHolder> {

    private Review[] items;
    private Context ctx;

    public AdapterReviews(Context context, Review[] items) {
        this.items = items;
        ctx = context;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public TextView tv_name;
        public TextView tv_review;
        public TextView tv_rating;

        public ViewHolder(View v) {
            super(v);
            tv_name = (TextView) v.findViewById(R.id.tv_name);
            tv_review = (TextView) v.findViewById(R.id.tv_review);
            tv_rating = (TextView) v.findViewById(R.id.tv_rating);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_review, parent, false);
        ViewHolder vh = new ViewHolder(v);
        return vh;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {
        final Review a = items[position];
        holder.tv_name.setText(a.authorName);
        holder.tv_rating.setText("( " + a.rating + " )");
        if (TextUtils.isEmpty(a.text)) {
            holder.tv_review.setVisibility(View.GONE);
        } else {
            holder.tv_review.setVisibility(View.VISIBLE);
        }
        holder.tv_review.setText(a.text);
    }

    @Override
    public int getItemCount() {
        return items.length;
    }
}
