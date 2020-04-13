package br.com.spod.spodvpnwebfilter;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.NumberFormat;
import java.util.Objects;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

public class AlertsGenericFragment extends Fragment implements AlertsGenericRecyclerAdapter.ItemClickListener, SwipeRefreshLayout.OnRefreshListener
{
    private static final String TAG = "AlertsGenericFragment";

    private static final int TRACKERS_PAGE = 0;
    private static final int THREATS_PAGE = 1;
    private static final int SITES_PAGE = 2;

    private int counter;
    private int page;

    private SwipeRefreshLayout mSwipeRefresh;
    private TextView counterView;

    private AlertsGenericRecyclerAdapter adapter;
    private AlertsFragment.AlertsPagerAdapter pagerAdapter;

    public static AlertsGenericFragment newInstance(int counter, int page, AlertsFragment.AlertsPagerAdapter pagerAdapter)
    {
        AlertsGenericFragment fragment = new AlertsGenericFragment();
        Bundle args = new Bundle();
        args.putInt("counter", counter);
        args.putInt("page", page);
        fragment.setArguments(args);
        fragment.pagerAdapter = pagerAdapter;
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            counter = getArguments().getInt("counter", 0);
            page = getArguments().getInt("page", 0);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.alerts_generic_fragment, container, false);

        //Setup counter label
        counterView = view.findViewById(R.id.alerts_generic_fragment_counter);
        counterView.setText(String.valueOf(counter));

        //Setup banner with title and icon
        TextView titleView = view.findViewById(R.id.alerts_generic_fragment_title);
        View bannerView = view.findViewById(R.id.alerts_generic_fragment_banner);
        ImageView iconView = view.findViewById(R.id.alerts_generic_fragment_icon);

        //Setup recycler view
        RecyclerView recyclerView = view.findViewById(R.id.alerts_generic_fragment_recycler_view);
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.addItemDecoration(new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL));

        if(adapter != null) {
            //Adapter is already initialized, simply reload
            adapter.notifyDataSetChanged();
        } else {
            //Create adapter
            adapter = new AlertsGenericRecyclerAdapter(getActivity(), null);
            adapter.setClickListener(this);
            if(pagerAdapter != null) pagerAdapter.adapters.add(adapter);
        }
        recyclerView.setAdapter(adapter);

        //Setup banner and icon
        if(page == TRACKERS_PAGE) {
            titleView.setText(R.string.alerts_blocked_trackers_title);
            bannerView.setBackground(ContextCompat.getDrawable(Objects.requireNonNull(getContext()), R.drawable.banner_blocked_trackers));
            iconView.setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.block_trackers_icon));
        }
        else if(page == THREATS_PAGE) {
            titleView.setText(R.string.alerts_blocked_threats_title);
            bannerView.setBackground(ContextCompat.getDrawable(Objects.requireNonNull(getContext()), R.drawable.banner_blocked_threats));
            iconView.setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.block_threats_icon_bw));
        } else {
            //SITES_PAGE
            titleView.setText(R.string.alerts_blocked_sites_title);
            bannerView.setBackground(ContextCompat.getDrawable(Objects.requireNonNull(getContext()), R.drawable.banner_blocked_sites));
            iconView.setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.block_sites_icon));
        }

        //Setup swipe refresh layout
        mSwipeRefresh = view.findViewById(R.id.alerts_generic_fragment_refresh);
        mSwipeRefresh.setOnRefreshListener(this);
        mSwipeRefresh.setNestedScrollingEnabled(true);

        //Refresh adapter's data once we get to the last (third) adapter
        if(page == 2) {
            mSwipeRefresh.post(() -> {
                mSwipeRefresh.setRefreshing(true);
                onRefresh();
            });
        }

        return view;
    }

    @Override public void onRefresh() { pagerAdapter.refreshAlerts(getActivity(), mSwipeRefresh); }

    @Override
    public void onItemClick(View view, int position)
    {
        //Show detail view
        String[] blockTypes = {getString(R.string.tracker), getString(R.string.threat), getString(R.string.site)};
        int index = position * 3;
        String hostname = (String)adapter.getItem(index+1);
        long timestamp = ((Number)adapter.getItem(index+2)).longValue() * 1000L;
        pagerAdapter.showDetail(Objects.requireNonNull(getActivity()), blockTypes[page], hostname, timestamp);
    }

    //Method for updating counter label
    @Override
    public void updateCounter(int count) {
        counter = count;
        counterView.setText(NumberFormat.getInstance().format(counter));
    }
}


