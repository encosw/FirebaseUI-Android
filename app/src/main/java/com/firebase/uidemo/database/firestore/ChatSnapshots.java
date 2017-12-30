package com.firebase.uidemo.database.firestore;

import android.support.annotation.NonNull;

import com.firebase.ui.firestore.ClassSnapshotParser;
import com.firebase.ui.firestore.FirestoreArray;
import com.firebase.ui.firestore.ObservableSnapshotArray;
import com.firebase.ui.firestore.SnapshotsHolder;
import com.google.firebase.firestore.Query;

public class ChatSnapshots extends SnapshotsHolder<Chat, Query> {
    @NonNull
    @Override
    protected ObservableSnapshotArray<Chat> createSnapshots(Query params) {
        return new FirestoreArray<>(params, new ClassSnapshotParser<>(Chat.class));
    }
}
