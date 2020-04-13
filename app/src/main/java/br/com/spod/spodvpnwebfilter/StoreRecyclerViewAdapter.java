package br.com.spod.spodvpnwebfilter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.android.billingclient.api.SkuDetails;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class StoreRecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>
{
    private LayoutInflater mInflater;
    private List<SkuDetails> mData;
    private ProductClickListener mClickListener;

    StoreRecyclerViewAdapter(Context context, List<SkuDetails> data) {
        this.mInflater = LayoutInflater.from(context);
        this.mData = data;
    }

    @Override
    public @NonNull RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
    {
        View view = mInflater.inflate(R.layout.store_recycler_view_row, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, final int position)
    {
        ProductViewHolder productViewHolder = (ProductViewHolder)holder;
        SkuDetails skuDetails = mData.get(position);
        productViewHolder.titleTextView.setText(skuDetails.getTitle().split("(?> \\(.+?\\))$")[0]); //remove redundant app name in product's title
        productViewHolder.subtitleTextView.setText(skuDetails.getDescription());
        productViewHolder.buyButton.setText(skuDetails.getPrice());
    }

    //Number of rows
    @Override
    public int getItemCount() {
        return (mData != null ? mData.size() : 0);
    }

    public class ProductViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener
    {
        TextView titleTextView, subtitleTextView;
        Button buyButton;

        ProductViewHolder(View productView) {
            super(productView);
            titleTextView = productView.findViewById(R.id.store_recycler_view_row_title);
            subtitleTextView = productView.findViewById(R.id.store_recycler_view_row_subtitle);
            buyButton = productView.findViewById(R.id.store_recycler_view_buy_button);
            productView.setOnClickListener(this);
            buyButton.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if (mClickListener != null) mClickListener.onItemClick(view, getAdapterPosition());
        }
    }

    //Get data at position
    SkuDetails getItem(int id) {
        return mData.get(id);
    }

    //Setup click listener
    void setClickListener(ProductClickListener productClickListener) {
        this.mClickListener = productClickListener;
    }

    //Click will be handled by parent
    public interface ProductClickListener {
        void onItemClick(View view, int position);
    }

    //Method for reloading list data
    void reloadData(List<SkuDetails> data) {
        mData = data;
        notifyDataSetChanged();
    }
}
