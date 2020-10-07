package br.com.spod.spodvpnwebfilter;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

public class MessageFragment extends Fragment
{
    private static final String TAG = "MessageFragment";

    private static final String ARG_MESSAGE_TEXT = "param1";
    private static final String ARG_AUTODESTRUCT = "param2";

    private String messageText;
    private long autoDestruct;

    //Required empty public constructor
    public MessageFragment() { }

    public static MessageFragment newInstance(String messageText, long autoDestruct)
    {
        MessageFragment fragment = new MessageFragment();
        Bundle args = new Bundle();
        args.putString(ARG_MESSAGE_TEXT, messageText);
        args.putLong(ARG_AUTODESTRUCT, autoDestruct);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            messageText = getArguments().getString(ARG_MESSAGE_TEXT);
            autoDestruct = getArguments().getLong(ARG_AUTODESTRUCT);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        //Inflate fragment's layout from XML
        View view = inflater.inflate(R.layout.fragment_message, container, false);

        //Setup view with message and close button
        TextView messageTextView = view.findViewById(R.id.fragment_message_text);
        messageTextView.setText(messageText);
        TextView closeButton = view.findViewById(R.id.fragment_message_close);
        MessageFragment fragment = this;

        closeButton.setOnClickListener(view1 -> {
            try {
                //close fragment and hide container
                requireActivity().getSupportFragmentManager().beginTransaction().remove(fragment).commit();
                FrameLayout frameLayout = requireActivity().findViewById(R.id.message_container);
                frameLayout.setVisibility(View.GONE);
            } catch (NullPointerException exception) {
                Log.v(TAG, "Got an exception while trying to close MessageFragment, most likely it was already closed, ignore!");
            } catch (IllegalStateException exception) {
                Log.v(TAG, "Got an IllegalStateException, probably running in the background...");
            }
        });

        //AutoDestruct to hide/close the message
        if(autoDestruct > 0)
        {
            final Handler handler = new Handler();
            handler.postDelayed(() -> {
                Log.v(TAG, "Auto-destruct: Closing message fragment!");
                closeButton.callOnClick();
            }, autoDestruct);
        }
        return view;
    }
}
