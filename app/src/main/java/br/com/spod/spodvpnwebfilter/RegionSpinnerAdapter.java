package br.com.spod.spodvpnwebfilter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Objects;

import androidx.annotation.NonNull;

public class RegionSpinnerAdapter extends ArrayAdapter<String>
{
    private final Context ctx;
    private String[] contentArray;
    private final Integer[] imageArray;

    public RegionSpinnerAdapter(Context context, int resource, String[] objects, Integer[] imageArray) {
        super(context, resource, R.id.spinnerTextView, objects);
        this.ctx = context;
        this.contentArray = objects;
        this.imageArray = imageArray;
    }

    @Override public View getDropDownView(int position, View convertView, @NonNull ViewGroup parent) {
        return getCustomView(position, parent);
    }

    @NonNull @Override public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        return getCustomView(position, parent);
    }

    public View getCustomView(int position, ViewGroup parent)
    {
        LayoutInflater inflater = (LayoutInflater)ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View row = Objects.requireNonNull(inflater).inflate(R.layout.region_spinner_layout, parent, false);

        TextView textView = row.findViewById(R.id.spinnerTextView);
        textView.setText(contentArray[position]);

        ImageView imageView = row.findViewById(R.id.spinnerImageView);
        imageView.setImageResource(imageArray[position]);

        return row;
    }

    public void setTextArray(String[] objects) {
        this.contentArray = objects;
        notifyDataSetChanged();
    }

}
