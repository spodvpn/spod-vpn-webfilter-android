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

import org.strongswan.android.data.VpnProfile;
import org.strongswan.android.data.VpnProfileDataSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import androidx.annotation.Dimension;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class MoreRecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>
{
    private static final int TYPE_SECTION = 0;
    private static final int TYPE_ITEM = 1;
    private static final int TYPE_BUTTON = 2;

    private static final int RESET_DOWNLOAD_UPLOAD_ROW = 7;
    private static final int APP_SECTION = 0;
    private static final int WEB_FILTER_SECTION = 8;
    private static final int HELP_SECTION = 14;
    private static final int COPYRIGHT_ROW = 20;
    private static final int CHANGE_LOG_ROW = 11;
    //private static final int SUB_INFO_ROW = 3;

    String subscriptionType, totalDownload, totalUpload, firewallVersion, firewallReleaseDate, changeLog, connectedServer;
    private final String appVersion, username;

    private final ArrayList<String> valuesList = new ArrayList<>();

    private final List<String> mData;
    private final LayoutInflater mInflater;
    private ItemClickListener mClickListener;

    //Constructor
    MoreRecyclerViewAdapter(Context context, List<String> data)
    {
        this.mInflater = LayoutInflater.from(context);
        this.mData = data;

        appVersion = BuildConfig.VERSION_NAME + '-' + BuildConfig.VERSION_CODE;

        SharedPreferences sharedPreferences = context.getSharedPreferences(context.getString(R.string.preferences_key), Context.MODE_PRIVATE);
        UUID uuid = UUID.nameUUIDFromBytes(Objects.requireNonNull(sharedPreferences.getString(context.getString(R.string.preferences_username), "")).getBytes());

        VpnProfileDataSource mDataSource = new VpnProfileDataSource(context);
        mDataSource.open();
        VpnProfile profile = mDataSource.getVpnProfile(uuid);
        if(profile != null && profile.getUsername() != null) username = profile.getUsername();
        else username = context.getString(R.string.not_available);
        mDataSource.close();

        //Initialize variables with N/A - These will be populated later from moreinfo API
        subscriptionType = context.getString(R.string.not_available);
        connectedServer = context.getString(R.string.not_available);
        totalDownload = context.getString(R.string.not_available);
        totalUpload = context.getString(R.string.not_available);
        firewallVersion = context.getString(R.string.not_available);
        firewallReleaseDate = context.getString(R.string.not_available);
    }

    @Override
    public int getItemViewType(int position) {
        if(position == APP_SECTION || position == WEB_FILTER_SECTION || position == HELP_SECTION)
            return TYPE_SECTION;
        else if (position < HELP_SECTION && position != RESET_DOWNLOAD_UPLOAD_ROW)
            return TYPE_ITEM;
        else
            return TYPE_BUTTON;
    }

    //Inflate row from XML
    @Override
    public @NonNull RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
    {
        View view;
        RecyclerView.ViewHolder viewHolder;

        //Clear and then [re]populate values array
        valuesList.clear();
        valuesList.add("section0-dummy-placeholder");
        valuesList.add(appVersion);
        valuesList.add(username);
        valuesList.add(subscriptionType);
        valuesList.add(connectedServer);
        valuesList.add(totalDownload);
        valuesList.add(totalUpload);
        valuesList.add("reset-download-upload-dummy-placeholder");
        valuesList.add("section1-dummy-placeholder");
        valuesList.add(firewallVersion);
        valuesList.add(firewallReleaseDate);
        valuesList.add(changeLog);

        if(viewType == TYPE_SECTION) {
            view = mInflater.inflate(R.layout.more_recycler_view_section, parent, false);
            viewHolder = new SectionViewHolder(view);
        }
        else if(viewType == TYPE_ITEM) {
            view = mInflater.inflate(R.layout.more_recycler_view_row, parent, false);
            viewHolder = new ItemViewHolder(view);
        }
        else {
            //TYPE_BUTTON
            view = mInflater.inflate(R.layout.more_recycler_view_button, parent, false);
            viewHolder = new ButtonViewHolder(view);
        }

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, final int position)
    {
        if(getItemViewType(position) == TYPE_SECTION) {
            SectionViewHolder sectionViewHolder = (SectionViewHolder)holder;
            String title = mData.get(position);
            sectionViewHolder.sectionTitle.setText(title);
        }
        else if(getItemViewType(position) == TYPE_ITEM)
        {
            ItemViewHolder itemViewHolder = (ItemViewHolder)holder;
            String text = mData.get(position);
            itemViewHolder.nameTextView.setText(text);
            itemViewHolder.nameTextView.setTypeface(Typeface.DEFAULT); //Remove bold

            //Only add a value to the positions that need them
            if(position < CHANGE_LOG_ROW && position != RESET_DOWNLOAD_UPLOAD_ROW) {
                itemViewHolder.valueTextView.setText(valuesList.get(position));
            } else {
                itemViewHolder.valueTextView.setText(""); //Temporary empty value
            }
        }
        else
            { //TYPE_BUTTON
            ButtonViewHolder buttonViewHolder = (ButtonViewHolder)holder;
            String title = mData.get(position);
            buttonViewHolder.buttonTitle.setText(title);
            buttonViewHolder.buttonTitle.setAllCaps(false);

            if(position == COPYRIGHT_ROW) {
                //last row (copyright)
                buttonViewHolder.buttonTitle.setTextAppearance(android.R.style.TextAppearance_Holo_Small);
                buttonViewHolder.buttonTitle.setTypeface(Typeface.DEFAULT); //Remove bold
                buttonViewHolder.buttonTitle.setTextSize(Dimension.SP, 12); //Make text smaller

                //Remove selectable background
                TypedValue outValue = new TypedValue();
                buttonViewHolder.buttonTitle.getContext().getTheme().resolveAttribute(android.R.attr.background, outValue, true);
                buttonViewHolder.buttonTitle.setBackgroundResource(outValue.resourceId);
            } else {
                //make sure style is correct (items are reused!)
                buttonViewHolder.buttonTitle.setTextAppearance(android.R.style.Widget_Material_Button_Borderless);
                buttonViewHolder.buttonTitle.setTextColor(Color.argb(255, 0, 133, 119));
                buttonViewHolder.buttonTitle.setTextSize(Dimension.SP, 14.0f);
                //Apply selectable background
                TypedValue outValue = new TypedValue();
                buttonViewHolder.buttonTitle.getContext().getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
                buttonViewHolder.buttonTitle.setBackgroundResource(outValue.resourceId);
            }
        }
    }

    //Number of rows
    @Override public int getItemCount() {
        return mData.size();
    }

    public class ItemViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener
    {
        TextView nameTextView, valueTextView;

        ItemViewHolder(View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.more_recycler_view_row_text);
            valueTextView = itemView.findViewById(R.id.more_recycler_view_row_value);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if (mClickListener != null) mClickListener.onItemClick(view, getBindingAdapterPosition());
        }
    }

    public static class SectionViewHolder extends RecyclerView.ViewHolder
    {
        TextView sectionTitle;

        SectionViewHolder(View sectionView) {
            super(sectionView);
            sectionTitle = sectionView.findViewById(R.id.more_recycler_view_section_title);
        }
    }

    public class ButtonViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener
    {
        TextView buttonTitle;

        ButtonViewHolder(View buttonView) {
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

    //Click will be handled by parent
    public interface ItemClickListener {
        void onItemClick(View view, int position);
    }

    //Custom reload data method to make sure even visible rows are updated (not the case with adapter.notifyDataSetChanged())
    void prepareForReload()
    {
        for(int i=0; i<mData.size(); i++)
        {
            mData.set(i, mData.get(i)+"TMP"); //add temporary suffix
            notifyItemChanged(i);
            mData.set(i, mData.get(i).replace("TMP","")); //remove suffix
            notifyItemChanged(i); //data has changed, adapter will reload row
        }
    }
}
