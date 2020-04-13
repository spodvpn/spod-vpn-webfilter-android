package br.com.spod.spodvpnwebfilter;

import android.graphics.Color;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.method.ScrollingMovementMethod;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.util.Objects;

import androidx.fragment.app.Fragment;

public class TermsFragment extends Fragment
{
    private static final String TAG = "TermsFragment";

    //private static final int TERMS_TYPE = 1; //Reserved for future
    static final int SUB_INFO_TYPE = 2;
    static final int CHANGELOG_TYPE = 3;

    private static final String ARG_TITLE = "Title";
    private static final String ARG_TEXT = "Text";
    private static final String ARG_TYPE = "Type";

    private String title;
    private String text;
    private int type;

    //Required empty public constructor
    public TermsFragment() { }

    public static TermsFragment newInstance(String title, String text, int type)
    {
        TermsFragment fragment = new TermsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TITLE, title);
        args.putString(ARG_TEXT, text);
        args.putInt(ARG_TYPE, type);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            title = getArguments().getString(ARG_TITLE);
            text = getArguments().getString(ARG_TEXT);
            type = getArguments().getInt(ARG_TYPE);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        //Inflate fragment layout from XML
        View view = inflater.inflate(R.layout.fragment_terms, container, false);

        TextView termsTitle = view.findViewById(R.id.terms_title);
        termsTitle.setText(title);
        TextView textView = view.findViewById(R.id.terms_text);
        textView.setText(text);
        textView.setMovementMethod(new ScrollingMovementMethod());

        if(type == SUB_INFO_TYPE)
        {
            textView.setBackgroundColor(Color.argb(255, 238, 238, 238)); //#eeeeee
            Spanned str = Html.fromHtml(text, 0);
            textView.setText(str);
            textView.setMovementMethod(LinkMovementMethod.getInstance()); //enable links?

            //modify textView's bottom constraint and also show 'Continue' button
            int val20dp = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20, getResources().getDisplayMetrics());
            int val105dp = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 105, getResources().getDisplayMetrics());

            ViewGroup.MarginLayoutParams newParams = (ViewGroup.MarginLayoutParams) textView.getLayoutParams();
            newParams.bottomMargin = val105dp;
            textView.setLayoutParams(newParams);

            //show 'Continue' button
            Button agreeButton = view.findViewById(R.id.terms_agree_button);
            agreeButton.setVisibility(View.VISIBLE);
            agreeButton.setOnClickListener(view1 -> {
                //Close this fragment, set flag in parent and start purchase method
                StoreFragment storeFragment = (StoreFragment) Objects.requireNonNull(getActivity()).getSupportFragmentManager().findFragmentByTag("StoreFragment");
                if (storeFragment != null) {
                    storeFragment.confirm_purchase();
                }
                getActivity().getSupportFragmentManager().popBackStackImmediate(); //close this fragment
            });
        }
        else if(type == CHANGELOG_TYPE) Objects.requireNonNull(getActivity()).setTitle(getString(R.string.more_tab_eleventh_item));

        return view;
    }
}
