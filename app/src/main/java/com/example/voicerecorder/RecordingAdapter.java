package com.example.voicerecorder;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.example.voicerecorder.model.Recording;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class RecordingAdapter extends BaseAdapter {
    private Context context;
    private List<Recording> recordings;
    private LayoutInflater inflater;

    public RecordingAdapter(Context context, List<Recording> recordings) {
        this.context = context;
        this.recordings = recordings;
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return recordings.size();
    }

    @Override
    public Object getItem(int position) {
        return recordings.get(position);
    }

    @Override
    public long getItemId(int position) {
        return recordings.get(position).getId();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_recording, parent, false);
            holder = new ViewHolder();
            holder.durationTextView = convertView.findViewById(R.id.durationTextView);
            holder.timeTextView = convertView.findViewById(R.id.timeTextView);
            holder.noteTextView = convertView.findViewById(R.id.noteTextView);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Recording recording = recordings.get(position);

        // Format duration
        long seconds = recording.getDuration() / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        holder.durationTextView.setText(String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds));

        // Format timestamp
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        holder.timeTextView.setText(sdf.format(recording.getTimestamp()));

        // Set note
        holder.noteTextView.setText(recording.getNote());

        return convertView;
    }

    private static class ViewHolder {
        TextView durationTextView;
        TextView timeTextView;
        TextView noteTextView;
    }

    public void updateRecordings(List<Recording> newRecordings) {
        this.recordings = newRecordings;
        notifyDataSetChanged();
    }
}
