package br.com.spod.spodvpnwebfilter;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

public class AlertsGenericRecyclerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>
{
    private static final String TAG = "AlertsAdapter";

    private static final int SUMMARY_PAGE = 0;
    private static final int TRACKERS_PAGE = 1;
    private static final int THREATS_PAGE = 2;
    private static final int SITES_PAGE = 3;

    private static final int ALERT_TYPE = 0;
    private static final int STAT_SINGLE_TYPE = 1;
    private static final int STAT_DOUBLE_TYPE = 2;

    private static final int MILLISECONDS_PER_BLOCK = 50;
    private static final long BYTES_PER_BLOCK = 150000L;

    private LayoutInflater mInflater;
    private ItemClickListener mClickListener;

    private ArrayList<Object> alertsArray;
    private Context fragmentContext;

    private int page;
    private AlertsFragment.AlertsPagerAdapter pagerAdapter;

    AlertsGenericRecyclerAdapter(Context context, ArrayList<Object> data, int pageNumber, AlertsFragment.AlertsPagerAdapter pagerAdapter) {
        this.mInflater = LayoutInflater.from(context);
        this.alertsArray = data;
        this.page = pageNumber;
        this.pagerAdapter = pagerAdapter;
        this.fragmentContext = context;
    }

    @Override
    public int getItemViewType(int position) {
        if(page == 0) {
            if(position == 0 || position == 2)
                return STAT_SINGLE_TYPE;
            else
                return STAT_DOUBLE_TYPE;
        }
        else
            return ALERT_TYPE;
    }

    @Override
    public @NonNull RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
    {
        View view;
        RecyclerView.ViewHolder viewHolder;

        if(page == SUMMARY_PAGE) {
            if(viewType == STAT_SINGLE_TYPE) {
                view = mInflater.inflate(R.layout.alerts_stats_single_row, parent, false);
                viewHolder = new SingleStatViewHolder(view);
            } else {
                view = mInflater.inflate(R.layout.alerts_stats_double_row, parent, false);
                viewHolder = new DoubleStatViewHolder(view);
            }
        } else {
            view = mInflater.inflate(R.layout.alerts_generic_recycler_view_row, parent, false);
            viewHolder = new AlertViewHolder(view);
        }

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, final int position)
    {
        if(page == SUMMARY_PAGE)
        {
            int nTrackers = Integer.parseInt(alertsArray.get(0).toString());
            int nThreats = Integer.parseInt(alertsArray.get(1).toString());
            int nSites = Integer.parseInt(alertsArray.get(2).toString());
            int nTotal = nTrackers + nThreats + nSites;

            //Time saved
            String totaltimeSaved;
            long millisecondsSaved = nTotal * MILLISECONDS_PER_BLOCK;
            long secondsSaved = millisecondsSaved/1000;
            long minutesSaved = secondsSaved/60;
            totaltimeSaved = String.format(Locale.getDefault(), "%dsec", secondsSaved);
            if(secondsSaved > 60) {
                totaltimeSaved = String.format(Locale.getDefault(), "%dmin", minutesSaved);
                if (minutesSaved > 60) {
                    long hoursSaved = minutesSaved / 60;
                    totaltimeSaved = String.format(Locale.getDefault(), "%dhrs", hoursSaved);
                }
            }

            //Data saved
            long bytesSaved = nTotal * BYTES_PER_BLOCK;
            GlobalMethods globalMethods = new GlobalMethods((FragmentActivity) fragmentContext);
            String totalDataSavedString = globalMethods.formatBytes(bytesSaved);

            //Battery saved
            double batterySaved = minutesSaved * 4.9; //0,026% por minuto (iP8)
            double batteryPercentage = batterySaved/1820 * 100; //1820mAh (iP8)
            String totalBatterySavedString = String.format(Locale.getDefault(), "%2.2f%%", batteryPercentage);

            if(position == 0) {
                //Row0 == Total blocked requests
                SingleStatViewHolder singleStatViewHolder = (SingleStatViewHolder)holder;
                String totalBlockedString;
                if(nTotal > 999) totalBlockedString = String.format(Locale.getDefault(), "%2.2fk", (float)nTotal/1000);
                else totalBlockedString = String.valueOf(nTotal);
                singleStatViewHolder.titleView.setText(totalBlockedString);
                singleStatViewHolder.descriptionView.setText(R.string.blocked_requests);
                singleStatViewHolder.iconView.setImageDrawable(ContextCompat.getDrawable(fragmentContext, R.drawable.block_icon));
            } else if(position == 1) {
                //Row1 == TimeSaved / BatterySaved
                DoubleStatViewHolder doubleStatViewHolder = (DoubleStatViewHolder)holder;
                doubleStatViewHolder.primaryTitleView.setText(totaltimeSaved);
                doubleStatViewHolder.primaryDescriptionView.setText(R.string.time_saved);
                doubleStatViewHolder.primaryIconView.setImageDrawable(ContextCompat.getDrawable(fragmentContext, R.drawable.clock_icon));
                doubleStatViewHolder.secondaryTitleView.setText(totalBatterySavedString);
                doubleStatViewHolder.secondaryDescriptionView.setText(R.string.battery);
                doubleStatViewHolder.secondaryIconView.setImageDrawable(ContextCompat.getDrawable(fragmentContext, R.drawable.battery_icon));
            } else if(position == 2) {
                //Row2 == Data saved
                SingleStatViewHolder singleStatViewHolder = (SingleStatViewHolder)holder;
                singleStatViewHolder.titleView.setText(totalDataSavedString);
                singleStatViewHolder.descriptionView.setText(R.string.saved);
                singleStatViewHolder.iconView.setImageDrawable(ContextCompat.getDrawable(fragmentContext, R.drawable.download_icon));
            } else if(position == 3) {
                //Row3 == Trackers / Threats
                DoubleStatViewHolder doubleStatViewHolder = (DoubleStatViewHolder)holder;
                String totalTrackersString;
                if(nTrackers > 999) totalTrackersString = String.format(Locale.getDefault(), "%2.2fk", (float)nTrackers/1000);
                else totalTrackersString = String.valueOf(nTrackers);
                doubleStatViewHolder.primaryTitleView.setText(totalTrackersString);
                doubleStatViewHolder.primaryDescriptionView.setText(R.string.trackers);
                doubleStatViewHolder.primaryIconView.setImageDrawable(ContextCompat.getDrawable(fragmentContext, R.drawable.tracking_icon));
                String totalThreatsString;
                if(nThreats > 999) totalThreatsString = String.format(Locale.getDefault(), "%2.2fk", (float)nThreats/1000);
                else totalThreatsString = String.valueOf(nThreats);
                doubleStatViewHolder.secondaryTitleView.setText(totalThreatsString);
                doubleStatViewHolder.secondaryDescriptionView.setText(R.string.threats);
                doubleStatViewHolder.secondaryIconView.setImageDrawable(ContextCompat.getDrawable(fragmentContext, R.drawable.threats_icon));
            }

        } else {

            AlertViewHolder alertViewHolder = (AlertViewHolder)holder;
            if (position == 499 && position == getItemCount() - 1) {
                //Only the first 500 alerts are initially fetched and this is where we fetch the rest
                alertViewHolder.titleView.setText("");
                alertViewHolder.subtitleView.setText(alertViewHolder.subtitleView.getResources().getString(R.string.loading));
                alertViewHolder.subtitleView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                alertViewHolder.subtitleView.setTag(199);
                alertViewHolder.subtitleView.setTypeface(alertViewHolder.subtitleView.getTypeface(), Typeface.BOLD);
                pagerAdapter.fullAlertsList = true;
                pagerAdapter.refreshAlerts((FragmentActivity) fragmentContext, null);
                return;
            }

            //Clear 'Loading...' cell
            if (alertViewHolder.subtitleView.getTag() == Integer.valueOf(199)) {
                alertViewHolder.subtitleView.setText("");
                alertViewHolder.subtitleView.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_START);
                alertViewHolder.subtitleView.setTag(0);
                alertViewHolder.subtitleView.setTypeface(alertViewHolder.subtitleView.getTypeface(), Typeface.NORMAL);
            }

            int index = position * 2;

            //Format timestamp
            long timestamp = ((Number) alertsArray.get(index)).longValue() * 1000L;
            Date date = new Date(timestamp);
            DateFormat dateFormat = new SimpleDateFormat("EEEE, d MMM yyyy HH:mm:ss", Locale.getDefault());
            String formattedDate = dateFormat.format(date);
            //Capitalize first char in string (BR region)
            if (formattedDate.substring(0, 1).equals(formattedDate.substring(0, 1).toLowerCase()))
                formattedDate = formattedDate.substring(0, 1).toUpperCase() + formattedDate.substring(1);
            alertViewHolder.titleView.setText(formattedDate);

            //List name as subtitle
            String subtitle;
            if (this.page == TRACKERS_PAGE)
                subtitle = String.format(alertViewHolder.titleView.getResources().getString(R.string.alerts_blocked_tracker), alertsArray.get(index + 1));
            else if (this.page == THREATS_PAGE)
                subtitle = String.format(alertViewHolder.titleView.getResources().getString(R.string.alerts_blocked_threat), alertsArray.get(index + 1));
            else
                subtitle = String.format(alertViewHolder.titleView.getResources().getString(R.string.alerts_blocked_site), alertsArray.get(index + 1));
            alertViewHolder.subtitleView.setText(subtitle);
        }
    }

    @Override
    public int getItemCount() {
        if(page == SUMMARY_PAGE) return (alertsArray != null ? 4 : 0);
        return (alertsArray != null ? alertsArray.size() / 2 : 0);
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
            if (mClickListener != null) mClickListener.onItemClick(view, getBindingAdapterPosition());
        }
    }

    public class SingleStatViewHolder extends RecyclerView.ViewHolder {
        TextView titleView;
        TextView descriptionView;
        ImageView iconView;

        SingleStatViewHolder(View singleStatView) {
            super(singleStatView);
            titleView = singleStatView.findViewById(R.id.alerts_stats_single_title);
            descriptionView = singleStatView.findViewById(R.id.alerts_stats_single_description);
            iconView = singleStatView.findViewById(R.id.alerts_stats_single_icon);
        }
    }

    public class DoubleStatViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView primaryTitleView, secondaryTitleView;
        TextView primaryDescriptionView, secondaryDescriptionView;
        ImageView primaryIconView, secondaryIconView;

        DoubleStatViewHolder(View doubleStatView) {
            super(doubleStatView);
            //Setup views and click listeners
            primaryTitleView = doubleStatView.findViewById(R.id.alerts_stats_double_primary_title);
            primaryTitleView.setOnClickListener(this);
            primaryDescriptionView = doubleStatView.findViewById(R.id.alerts_stats_double_primary_description);
            primaryDescriptionView.setOnClickListener(this);
            primaryIconView = doubleStatView.findViewById(R.id.alerts_stats_double_primary_icon);
            primaryIconView.setOnClickListener(this);
            secondaryTitleView = doubleStatView.findViewById(R.id.alerts_stats_double_secondary_title);
            secondaryTitleView.setOnClickListener(this);
            secondaryDescriptionView = doubleStatView.findViewById(R.id.alerts_stats_double_secondary_description);
            secondaryDescriptionView.setOnClickListener(this);
            secondaryIconView = doubleStatView.findViewById(R.id.alerts_stats_double_secondary_icon);
            secondaryIconView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view)
        {
            if(getBindingAdapterPosition() != 3) return; //Only last row should be clickable
            int viewId = view.getId();
            int pageNumber = 0;
            if (viewId == R.id.alerts_stats_double_primary_title || viewId == R.id.alerts_stats_double_primary_description || viewId == R.id.alerts_stats_double_primary_icon)
                pageNumber = 1; //Trackers page
            else
                pageNumber = 2; //Threats page

            try {
                FragmentActivity context = (FragmentActivity) fragmentContext;
                AlertsFragment alertsFragment = (AlertsFragment) context.getSupportFragmentManager().findFragmentByTag("AlertsFragment");
                ViewPager viewPager = alertsFragment.getView().findViewById(R.id.alerts_fragment_view_pager);
                viewPager.setCurrentItem(pageNumber);
            } catch (Exception exception) {
                exception.printStackTrace();
            }
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
    void reloadData(ArrayList<Object> arrayList, Integer totalCount)
    {
        alertsArray = arrayList;
        notifyDataSetChanged();
        mClickListener.updateCounter(totalCount);
    }
}
