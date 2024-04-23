package br.com.spod.spodvpnwebfilter;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.android.billingclient.api.ProductDetails;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class StoreRecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>
{
    private final LayoutInflater mInflater;
    private List<ProductDetails> mData;
    private List<String> mFreeTrialData;
    private ProductClickListener mClickListener;

    StoreRecyclerViewAdapter(Context context, List<ProductDetails> data) {
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
        if(mFreeTrialData != null && position == 0) {
            productViewHolder.titleTextView.setText(mFreeTrialData.get(0));
            productViewHolder.subtitleTextView.setText(mFreeTrialData.get(1));
            productViewHolder.buyButton.setText(mFreeTrialData.get(2));
        } else {
            ProductDetails productDetails = (mFreeTrialData != null) ? mData.get(position-1) : mData.get(position);
            productViewHolder.titleTextView.setText(productDetails.getTitle().split("(?> \\(.+?\\))$")[0]); //remove redundant app name in product's title
            productViewHolder.subtitleTextView.setText(productDetails.getDescription());
            assert productDetails.getSubscriptionOfferDetails() != null;
            productViewHolder.buyButton.setText(productDetails.getSubscriptionOfferDetails().get(0).getPricingPhases().getPricingPhaseList().get(0).getFormattedPrice());
        }
    }

    //Number of rows
    @Override
    public int getItemCount() {
        if(mData == null) return 0;
        return (mFreeTrialData != null ? mData.size() + 1 : mData.size());
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
            if (mClickListener != null) mClickListener.onItemClick(view, getBindingAdapterPosition());
        }
    }

    //Get data at position
    Object getItem(int id) {
        if(mFreeTrialData == null) {
            return mData.get(id);
        }
        return id == 0 ? mFreeTrialData : mData.get(id-1);
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
    void reloadData(List<ProductDetails> data, List<String> freeTrialInfo)
    {
        mData = data;
        mFreeTrialData = freeTrialInfo;

        final Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(this::notifyDataSetChanged, 500);
    }
}
