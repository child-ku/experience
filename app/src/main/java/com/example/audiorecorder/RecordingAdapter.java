package com.example.audiorecorder;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;

import java.util.List;

public class RecordingAdapter extends BaseAdapter {

    private Context mContext;
    private List<RecordingItem> mRecordingList;
    private OnItemClickListener mListener;

    public interface OnItemClickListener {
        void onItemClick(RecordingItem item);
        void onEditClick(RecordingItem item);
        void onDeleteClick(RecordingItem item);
    }

    public RecordingAdapter(Context context, List<RecordingItem> recordingList) {
        mContext = context;
        mRecordingList = recordingList;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        mListener = listener;
    }

    @Override
    public int getCount() {
        return mRecordingList.size();
    }

    @Override
    public Object getItem(int position) {
        return mRecordingList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(R.layout.item_recording, parent, false);
            holder = new ViewHolder();
            holder.textDuration = convertView.findViewById(R.id.text_duration);
            holder.textTime = convertView.findViewById(R.id.text_time);
            holder.textNote = convertView.findViewById(R.id.text_note);
            holder.btnEdit = convertView.findViewById(R.id.btn_edit);
            holder.btnDelete = convertView.findViewById(R.id.btn_delete);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        final RecordingItem item = mRecordingList.get(position);

        holder.textDuration.setText(item.getFormattedDuration());
        holder.textTime.setText(item.getTime());
        holder.textNote.setText(item.getNote());

        convertView.setOnClickListener(v -> {
            if (mListener != null) {
                mListener.onItemClick(item);
            }
        });

        holder.btnEdit.setOnClickListener(v -> {
            if (mListener != null) {
                mListener.onEditClick(item);
            }
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (mListener != null) {
                mListener.onDeleteClick(item);
            }
        });

        return convertView;
    }

    private static class ViewHolder {
        TextView textDuration;
        TextView textTime;
        TextView textNote;
        Button btnEdit;
        Button btnDelete;
    }
}