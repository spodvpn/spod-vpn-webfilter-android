package br.com.spod.spodvpnwebfilter;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import androidx.annotation.Nullable;

public class ConfirmBottomDialogFragment extends BottomSheetDialogFragment
{
    static final int TYPE_RESET = 0;
    static final int TYPE_UNBLOCK = 1;

    private final String title, subtitle, confirm_button, deny_button;
    private final int type;

    private ConfirmBottomDialogFragment(int type, String title, String subtitle, String confirm_button, String deny_button) {
        this.type = type;
        this.title = title;
        this.subtitle = subtitle;
        this.confirm_button = confirm_button;
        this.deny_button = deny_button;
    }

    static ConfirmBottomDialogFragment newInstance(int type, String title, String subtitle, String confirm_button, String deny_button) {
        return new ConfirmBottomDialogFragment(type, title, subtitle, confirm_button, deny_button);
    }

    @Nullable @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.bottom_sheet, container, false);

        //Setup title, subtitle, confirm and deny buttons
        TextView titleView = view.findViewById(R.id.bottom_sheet_title);
        titleView.setText(this.title);

        TextView subtitleView = view.findViewById(R.id.bottom_sheet_subtitle);
        subtitleView.setText(this.subtitle);

        TextView cancelTextView = view.findViewById(R.id.bottom_sheet_cancel);
        cancelTextView.setText(this.deny_button);

        TextView confirmTextView = view.findViewById(R.id.bottom_sheet_confirm);
        confirmTextView.setText(this.confirm_button);
        confirmTextView.setOnClickListener(view1 -> {
            if(type == TYPE_RESET)
            {
                //Get current timestamp and store in sharedPreferences
                long timestamp = System.currentTimeMillis();
                SharedPreferences sharedPreferences = requireContext().getSharedPreferences(getString(R.string.preferences_key), Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putLong(getString(R.string.preferences_reset_download_upload), timestamp);
                editor.apply();

                //Refresh more info
                MoreFragment parentFragment = ((MoreFragment) ConfirmBottomDialogFragment.this.getParentFragment());
                if (parentFragment != null) {
                    parentFragment.adapter.totalDownload = "";
                    parentFragment.adapter.totalUpload = "";
                    parentFragment.onRefresh();
                }
            }
            else if(type == TYPE_UNBLOCK)
            {
                //Parent will handle it
                AlertDetailFragment parentFragment = ((AlertDetailFragment) ConfirmBottomDialogFragment.this.getParentFragment());
                if (parentFragment != null) {
                    parentFragment.unblockHostname();
                }
            }
            dismiss();
        });

        cancelTextView.setOnClickListener(view12 -> dismiss());

        return view;
    }
}