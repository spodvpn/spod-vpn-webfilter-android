package br.com.spod.spodvpnwebfilter;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import androidx.annotation.Dimension;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class AlertDetailRecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>
{
    private static final int TYPE_ITEM = 0;
    private static final int TYPE_BUTTON = 1;

    private static final int UNBLOCK_HOSTNAME_ROW = 3;

    private List<String> mData;
    private LayoutInflater mInflater;
    private ItemClickListener mClickListener;

    private String blockType, hostname;
    private Long timestamp;

    AlertDetailRecyclerViewAdapter(Context context, List<String> data, String blockType, String hostname, Long timestamp)
    {
        this.mInflater = LayoutInflater.from(context);
        this.mData = data;
        this.blockType = blockType;
        this.hostname = hostname;
        this.timestamp = timestamp;
    }

    @Override
    public int getItemViewType(int position) {
        return (position == UNBLOCK_HOSTNAME_ROW ? TYPE_BUTTON : TYPE_ITEM);
    }

    @Override
    public @NonNull RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
    {
        View view;
        RecyclerView.ViewHolder viewHolder;

        if(viewType == TYPE_ITEM) {
            view = mInflater.inflate(R.layout.more_recycler_view_row, parent, false);
            viewHolder = new DetailViewHolder(view);
        } else {
            view = mInflater.inflate(R.layout.more_recycler_view_button, parent, false);
            viewHolder = new DetailButtonViewHolder(view);
        }

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, final int position)
    {
        if(getItemViewType(position) == TYPE_ITEM)
        {
            DetailViewHolder detailViewHolder = (DetailViewHolder)holder;
            String text = mData.get(position);
            detailViewHolder.nameTextView.setText(text);
            detailViewHolder.nameTextView.setTypeface(Typeface.DEFAULT); //Remove bold

            //Format timestamp
            Date blockDate = new Date(timestamp);
            SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, d MMM yyyy", Locale.getDefault());
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

            switch (position) {
                case 0: //Hostname
                    detailViewHolder.valueTextView.setText(hostname);
                    break;
                case 1: //Date
                    detailViewHolder.valueTextView.setText(dateFormat.format(blockDate));
                    break;
                case 2: //Time
                    detailViewHolder.valueTextView.setText(timeFormat.format(blockDate));
                    break;
            }
        }
        else {
            //TYPE_BUTTON
            DetailButtonViewHolder detailButtonViewHolder = (DetailButtonViewHolder)holder;
            String title = mData.get(position);
            detailButtonViewHolder.buttonTitle.setText(title);
            detailButtonViewHolder.buttonTitle.setAllCaps(false);
            detailButtonViewHolder.buttonTitle.setTextAppearance(android.R.style.Widget_Material_Button_Borderless);
            detailButtonViewHolder.buttonTitle.setTextSize(Dimension.SP, 14.0f);

            //Check if it's already in the 'Unblocked sites' list first
            SharedPreferences preferences = detailButtonViewHolder.buttonTitle.getContext().getSharedPreferences(detailButtonViewHolder.buttonTitle.getContext().getString(R.string.preferences_key), Context.MODE_PRIVATE);
            Set<String> unblocked_list_set = preferences.getStringSet(detailButtonViewHolder.buttonTitle.getContext().getString(R.string.preferences_whitelist_key), null);
            Set<String> blocked_list_set = preferences.getStringSet(detailButtonViewHolder.buttonTitle.getContext().getString(R.string.preferences_blacklist_key), null);

            if((unblocked_list_set != null && unblocked_list_set.contains(hostname)) || (blockType.equals(detailButtonViewHolder.buttonTitle.getContext().getString(R.string.site)) && blocked_list_set != null && ! blocked_list_set.contains(hostname))) {
                //Hostname is already in the 'Unblocked Sites' list OR it's a custom hostname not in the 'Blocked Sites' list: Make button disabled/greyed out
                detailButtonViewHolder.buttonTitle.setTextColor(Color.argb(255, 102, 102, 102));
                TypedValue outValue = new TypedValue();
                detailButtonViewHolder.buttonTitle.getContext().getTheme().resolveAttribute(android.R.attr.background, outValue, true);
                detailButtonViewHolder.buttonTitle.setBackgroundResource(outValue.resourceId);
            }
            else {
                detailButtonViewHolder.buttonTitle.setTextColor(Color.argb(255, 0, 133, 119));
                //Apply selectable background
                TypedValue outValue = new TypedValue();
                detailButtonViewHolder.buttonTitle.getContext().getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
                detailButtonViewHolder.buttonTitle.setBackgroundResource(outValue.resourceId);
            }
        }
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    public static class DetailViewHolder extends RecyclerView.ViewHolder
    {
        TextView nameTextView, valueTextView;

        DetailViewHolder(View detailView) {
            super(detailView);
            nameTextView = detailView.findViewById(R.id.more_recycler_view_row_text);
            valueTextView = detailView.findViewById(R.id.more_recycler_view_row_value);
        }
    }

    public class DetailButtonViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView buttonTitle;

        DetailButtonViewHolder(View buttonView) {
            super(buttonView);
            buttonTitle = buttonView.findViewById(R.id.more_recycler_view_button);
            buttonTitle.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if (mClickListener != null) mClickListener.onItemClick(view, getBindingAdapterPosition());
        }
    }

    //Setup click listener
    void setClickListener(ItemClickListener itemClickListener) {
        this.mClickListener = itemClickListener;
    }

    //Click wil be handled by parent
    public interface ItemClickListener {
        void onItemClick(View view, int position);
    }
}
