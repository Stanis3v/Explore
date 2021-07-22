package com.app.explore.adapter;

import android.content.Context;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.app.explore.R;
import com.app.explore.model.Around;

import java.util.ArrayList;
import java.util.List;

public class AdapterAroundMe extends RecyclerView.Adapter<AdapterAroundMe.ViewHolder> {

    private List<Around> items = new ArrayList<>();
    private Context ctx;
    private OnItemClickListener mOnItemClickListener;

    public interface OnItemClickListener {
        void onItemClick(View view, Around obj, int position);
    }

    public void setOnItemClickListener(final OnItemClickListener mItemClickListener) {
        this.mOnItemClickListener = mItemClickListener;
    }

    public AdapterAroundMe(Context context, List<Around> items) {
        this.items = items;
        ctx = context;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public ImageView img_a_icon;
        public TextView tv_a_name;
        public RelativeLayout view_icon;
        public LinearLayout lyt_parent;

        public ViewHolder(View v) {
            super(v);
            img_a_icon = (ImageView) v.findViewById(R.id.img_a_icon);
            tv_a_name = (TextView) v.findViewById(R.id.tv_a_name);
            view_icon = (RelativeLayout) v.findViewById(R.id.view_icon);
            lyt_parent = (LinearLayout) v.findViewById(R.id.lyt_parent);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_arround, parent, false);
        ViewHolder vh = new ViewHolder(v);
        return vh;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {
        final Around a = items.get(position);
        holder.tv_a_name.setText(a.name);
        if (a.icon != -1) {
            holder.img_a_icon.setImageResource(a.icon);
        } else {
            holder.view_icon.setVisibility(View.GONE);
        }

        holder.lyt_parent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mOnItemClickListener != null) {
                    mOnItemClickListener.onItemClick(view, a, position);
                }
            }
        });
    }

    public void setListData(List<Around> items) {
        this.items.clear();
        this.items = items;
        notifyDataSetChanged();
    }

    public void resetListData() {
        this.items = new ArrayList<>();
        notifyDataSetChanged();
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return items.size();
    }
}
