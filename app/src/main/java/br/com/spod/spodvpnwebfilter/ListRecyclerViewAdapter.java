package br.com.spod.spodvpnwebfilter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class ListRecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>
{
    private static final int UNBLOCK_LIST_TYPE = 0; //Whitelist
    //private static final int BLOCK_LIST_TYPE = 1; //Blacklist

    private static final int TYPE_ITEM = 0;
    private static final int TYPE_FOOTER = 1;

    private final int listType;
    private final Context this_context;

    private List<String> mData;
    private final LayoutInflater mInflater;
    private ListRecyclerViewAdapter.ItemClickListener mClickListener;

    //Constructor
    ListRecyclerViewAdapter(Context context, List<String> data, int type)
    {
        this.mInflater = LayoutInflater.from(context);
        //Add footer with information
        data.add((type == UNBLOCK_LIST_TYPE ? context.getString(R.string.custom_list_unblock_description) : context.getString(R.string.custom_list_block_description)));
        this.mData = data;
        this_context = context;
        listType = type;
    }

    @Override
    public int getItemViewType(int position) {
        return (position < mData.size()-1 ? TYPE_ITEM : TYPE_FOOTER);
    }

    //Inflate view from XML
    @Override
    public @NonNull
    RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
    {
        View view;
        RecyclerView.ViewHolder viewHolder;

        if(viewType == TYPE_ITEM) {
            view = mInflater.inflate(R.layout.custom_list_recycler_view, parent, false);
            viewHolder = new HostnameViewHolder(view);
        } else { //TYPE_FOOTER
            view = mInflater.inflate(R.layout.custom_list_recycler_view_footer, parent, false);
            viewHolder = new FooterViewHolder(view);
        }

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, final int position)
    {
        if(getItemViewType(position) == TYPE_ITEM)
        {
            HostnameViewHolder hostnameViewHolder = (HostnameViewHolder) holder;
            String hostname = mData.get(position);
            hostnameViewHolder.hostnameTextView.setText(hostname);
        } else
            { //TYPE_FOOTER
            FooterViewHolder footerViewHolder = (FooterViewHolder) holder;
            footerViewHolder.footerText.setText(mData.get(position));
        }
    }

    @Override public int getItemCount() {
        return mData.size();
    }

    public class HostnameViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener
    {
        TextView hostnameTextView;
        ImageButton removeHostnameButton;

        HostnameViewHolder(View hostnameView) {
            super(hostnameView);
            hostnameTextView = hostnameView.findViewById(R.id.custom_list_recycler_view_row);
            removeHostnameButton = hostnameView.findViewById(R.id.custom_list_remove_button);
            removeHostnameButton.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if (mClickListener != null) mClickListener.onItemClick(view, getBindingAdapterPosition());
        }
    }

    public static class FooterViewHolder extends RecyclerView.ViewHolder
    {
        TextView footerText;

        FooterViewHolder(View footerView) {
            super(footerView);
            footerText = footerView.findViewById(R.id.custom_list_recycler_view_footer);
        }
    }

    //Get data at position
    String getItem(int id) {
        return mData.get(id);
    }

    //Setup click listener
    void setClickListener(ItemClickListener itemClickListener) {
        this.mClickListener = itemClickListener;
    }

    //Click will be handled by parent
    public interface ItemClickListener {
        void onItemClick(View view, int position);
    }

    //Method for reloading list
    void reloadData(Set<String> listSet)
    {
        ArrayList<String> rowsList = new ArrayList<>();
        ArrayList<String> listArray = new ArrayList<>(listSet);

        for(int i=0; i<listSet.size(); i++) {
            if (listArray.get(i).equals("")) continue;
            rowsList.add(listArray.get(i));
        }

        mData.clear();
        rowsList.add((listType == UNBLOCK_LIST_TYPE ? this_context.getString(R.string.custom_list_unblock_description) : this_context.getString(R.string.custom_list_block_description)));
        mData = rowsList;

        notifyDataSetChanged();
    }
}

