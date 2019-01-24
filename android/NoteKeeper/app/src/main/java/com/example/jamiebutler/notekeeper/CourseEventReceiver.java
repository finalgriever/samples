package com.example.jamiebutler.notekeeper;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * This receiver would work in another app
 */

public class CourseEventReceiver extends BroadcastReceiver {
    public static final String ACTION_COURSE_EVENT = "com.example.jamiebutler.notekeeper.action.COURSE_EVENT";
    public static final String EXTRA_COURSE_ID = "com.example.jamiebutler.notekeeper.action.COURSE_ID";
    public static final String EXTRA_COURSE_MESSAGE = "com.example.jamiebutler.notekeeper.action.COURSE_MESSAGE";

//    In the onCreate of the component that needs to consume the messages, you'll need to provide and call this method:
//    In onDestroy, call unregisterReceiver(mCourseEventsReceiver)
//
//    private void setupCourseEventReceiver() {
//        mCourseEventsReceiver = new CourseEventsReceiver();
//        mCourseEventsReceiver.setCourseEventsDisplayCallbacks(this);
//        IntentFilter filter = new IntentFilter(CourseEventReceiver.ACTION_COURSE_EVENT)
//        registerReceiver(mCourseEventsReceiver, filter);
//    }

    private CourseEventsDisplayCallbacks mCourseEventsDisplayCallbacks;

    @Override
    public void onReceive(Context context, Intent intent) {
        if(!ACTION_COURSE_EVENT.equals(intent.getAction())) {
            return;
        }

        String courseId = intent.getStringExtra(EXTRA_COURSE_ID);
        String courseMessage = intent.getStringExtra(EXTRA_COURSE_MESSAGE);

        if(mCourseEventsDisplayCallbacks != null) {
            mCourseEventsDisplayCallbacks.onEventReceived(courseId, courseMessage);
        }
    }

    public void setmCourseEventsDisplayCallbacks(CourseEventsDisplayCallbacks mCourseEventsDisplayCallbacks) {
        this.mCourseEventsDisplayCallbacks = mCourseEventsDisplayCallbacks;
    }
}
