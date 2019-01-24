package com.example.jamiebutler.notekeeper;

import android.app.Service;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.IBinder;

public class NoteUploaderJobService extends JobService {
    public static final String EXTRA_DATA_URI = "com.example.jamiebutler.notekeeper.extras.JOB_DATA_URI";
    private NoteUploader mNoteUploader;

    public NoteUploaderJobService() {
    }

    @Override
    public boolean onStartJob(JobParameters params) {
        mNoteUploader = new NoteUploader(this);
        AsyncTask<JobParameters, Void, Void> task = new AsyncTask<JobParameters, Void, Void>() {
            @Override
            protected Void doInBackground(JobParameters... backgroundParams) {
                JobParameters jobParams = backgroundParams[0];
                Uri dataUri = Uri.parse(jobParams.getExtras().getString(EXTRA_DATA_URI));
                mNoteUploader.doUpload(dataUri);
                if(!mNoteUploader.isCanceled()) {
                    jobFinished(jobParams, false);
                }
                return null;
            }
        };

        task.execute(params);
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        mNoteUploader.cancel();
        return true;
    }

}
