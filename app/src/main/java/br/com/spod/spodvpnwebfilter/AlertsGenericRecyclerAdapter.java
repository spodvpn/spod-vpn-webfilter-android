package br.com.spod.spodvpnwebfilter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class AlertsGenericRecyclerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>
{
    private static final String TAG = "AlertsAdapter";

    private LayoutInflater mInflater;
    private ItemClickListener mClickListener;

    private ArrayList<Object> alertsArray;

    AlertsGenericRecyclerAdapter(Context context, ArrayList<Object> data) {
        this.mInflater = LayoutInflater.from(context);
        this.alertsArray = data;
    }

    @Override
    public @NonNull RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
    {
        View view = mInflater.inflate(R.layout.alerts_generic_recycler_view_row, parent, false);
        return new AlertViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, final int position)
    {
        AlertViewHolder alertViewHolder = (AlertViewHolder)holder;

        int index = position * 3;

        //Format timestamp
        long timestamp = ((Number)alertsArray.get(index+2)).longValue() * 1000L;
        Date date = new Date(timestamp);
        DateFormat dateFormat = new SimpleDateFormat("EEEE, d MMM yyyy HH:mm:ss", Locale.getDefault());
        String formattedDate = dateFormat.format(date);
        //Capitalize first chracter in string (BR region)
        if(formattedDate.substring(0, 1).equals(formattedDate.substring(0, 1).toLowerCase()))
            formattedDate = formattedDate.substring(0, 1).toUpperCase() + formattedDate.substring(1);
        alertViewHolder.titleView.setText(formattedDate);

        //List name as subtitle
        String subtitle;
        String list = (String)alertsArray.get(index);
        if(list.equals("block-tracking"))
            subtitle = String.format(alertViewHolder.titleView.getResources().getString(R.string.alerts_blocked_tracker), alertsArray.get(index+1));
         else if(list.equals("malware-phishing"))
             subtitle = String.format(alertViewHolder.titleView.getResources().getString(R.string.alerts_blocked_threat), alertsArray.get(index+1));
         else
            subtitle = String.format(alertViewHolder.titleView.getResources().getString(R.string.alerts_blocked_site), alertsArray.get(index+1));
         alertViewHolder.subtitleView.setText(subtitle);
    }

    @Override
    public int getItemCount() {
        return (alertsArray != null ? alertsArray.size() / 3 : 0);
    }

    public class AlertViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView titleView;
        TextView subtitleView;

        AlertViewHolder(View alertView) {
            super(alertView);
            titleView = alertView.findViewById(R.id.alerts_generic_recycler_view_title);
            subtitleView = alertView.findViewById(R.id.alerts_generic_recycler_view_subtitle);
            alertView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if (mClickListener != null) mClickListener.onItemClick(view, getAdapterPosition());
        }
    }

    //Get data at position
    Object getItem(int id) {
        return alertsArray.get(id);
    }

    //Setup click listener
    void setClickListener(ItemClickListener itemClickListener) {
        this.mClickListener = itemClickListener;
    }

    //Click wil be handled by parent
    public interface ItemClickListener {
        void onItemClick(View view, int position);
        void updateCounter(int count);
    }

    //Method for reloading alerts list
    void reloadData(ArrayList<Object> arrayList)
    {
        alertsArray = arrayList;
        notifyDataSetChanged();
        mClickListener.updateCounter(alertsArray.size()/3);
    }
}
