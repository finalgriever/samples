package com.example.jamiebutler.notekeeper;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.jamiebutler.notekeeper.NoteKeeperDatabaseContract.CourseInfoEntry;

public class CourseRecyclerAdapter extends RecyclerView.Adapter<CourseRecyclerAdapter.ViewHolder> {
    private final Context mContext;
    private LayoutInflater mLayoutInflater;
    private Cursor mCursor;
    private int mCourseTitlePos;
    private int mIdPos;

    public CourseRecyclerAdapter(Context mContext, Cursor coursesCursor) {
        this.mContext = mContext;
        mLayoutInflater = LayoutInflater.from(mContext);
        this.mCursor = coursesCursor;
    }

    private void populateColumnPositions() {
        if(mCursor == null)
            return;
        mCourseTitlePos = mCursor.getColumnIndex(CourseInfoEntry.COLUMN_COURSE_TITLE);
        mIdPos = mCursor.getColumnIndex(CourseInfoEntry._ID);
    }

    public void changeCursor(Cursor cursor) {
        if(mCursor != null)
            mCursor.close();
        mCursor = cursor;
        populateColumnPositions();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CourseRecyclerAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = mLayoutInflater.inflate(R.layout.item_course_list, parent, false);
        return new CourseRecyclerAdapter.ViewHolder(itemView);
    }


    @Override
    public void onBindViewHolder(@NonNull CourseRecyclerAdapter.ViewHolder holder, int position) {
        mCursor.moveToPosition(position);
        String courseTitle = mCursor.getString(mCourseTitlePos);
        int id = mCursor.getInt(mIdPos);

        holder.mTextCourse.setText(courseTitle);
        holder.mCourseTitle = courseTitle;
    }

    @Override
    public int getItemCount() {
        if(mCursor == null) return 0;
        return mCursor.getCount();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        public final TextView mTextCourse;
        public String mCourseTitle;

        public ViewHolder(View itemView) {
            super(itemView);
            mTextCourse = itemView.findViewById(R.id.text_course);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Snackbar.make(v, mCourseTitle, Snackbar.LENGTH_LONG).show();
                }
            });
        }
    }
}
