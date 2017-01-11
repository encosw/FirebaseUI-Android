/*
 * Copyright 2016 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.firebase.ui.database;

import android.support.annotation.NonNull;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

/**
 * This class implements an array-like collection on top of a Firebase location.
 */
class FirebaseArray implements ChildEventListener, ValueEventListener {
    public interface ChangeEventListener {
        enum EventType {ADDED, CHANGED, REMOVED, MOVED}

        void onChildChanged(EventType type, int index, int oldIndex);

        void onDataChanged();

        void onCancelled(DatabaseError error);
    }

    protected ChangeEventListener mListener;
    protected boolean mIsListening;
    private Query mQuery;
    private List<DataSnapshot> mSnapshots = new ArrayList<>();

    public FirebaseArray(Query ref) {
        mQuery = ref;
    }

    public void setChangeEventListener(@NonNull ChangeEventListener listener) {
        if (mIsListening && listener == null) {
            throw new IllegalStateException("Listener cannot be null.");
        }
        mListener = listener;
    }

    public void startListening() {
        if (mListener == null) throw new IllegalStateException("Listener cannot be null.");
        mQuery.addChildEventListener(this);
        mQuery.addValueEventListener(this);
        mIsListening = true;
    }

    public void stopListening() {
        mQuery.removeEventListener((ValueEventListener) this);
        mQuery.removeEventListener((ChildEventListener) this);
        mSnapshots.clear();
        mIsListening = false;
    }

    public int size() {
        return mSnapshots.size();
    }

    public DataSnapshot get(int index) {
        return mSnapshots.get(index);
    }

    @Override
    public void onChildAdded(DataSnapshot snapshot, String previousChildKey) {
        int index = 0;
        if (previousChildKey != null) {
            index = getIndexForKey(previousChildKey) + 1;
        }
        mSnapshots.add(index, snapshot);
        notifyChangeListener(ChangeEventListener.EventType.ADDED, index);
    }

    @Override
    public void onChildChanged(DataSnapshot snapshot, String previousChildKey) {
        int index = getIndexForKey(snapshot.getKey());
        mSnapshots.set(index, snapshot);
        notifyChangeListener(ChangeEventListener.EventType.CHANGED, index);
    }

    @Override
    public void onChildRemoved(DataSnapshot snapshot) {
        int index = getIndexForKey(snapshot.getKey());
        mSnapshots.remove(index);
        notifyChangeListener(ChangeEventListener.EventType.REMOVED, index);
    }

    @Override
    public void onChildMoved(DataSnapshot snapshot, String previousChildKey) {
        int oldIndex = getIndexForKey(snapshot.getKey());
        mSnapshots.remove(oldIndex);
        int newIndex = previousChildKey == null ? 0 : (getIndexForKey(previousChildKey) + 1);
        mSnapshots.add(newIndex, snapshot);
        mListener.onChildChanged(ChangeEventListener.EventType.MOVED, newIndex, oldIndex);
    }

    @Override
    public void onDataChange(DataSnapshot dataSnapshot) {
        mListener.onDataChanged();
    }

    @Override
    public void onCancelled(DatabaseError error) {
        mListener.onCancelled(error);
    }

    private int getIndexForKey(String key) {
        int index = 0;
        for (DataSnapshot snapshot : mSnapshots) {
            if (snapshot.getKey().equals(key)) {
                return index;
            } else {
                index++;
            }
        }
        throw new IllegalArgumentException("Key not found");
    }

    protected void notifyChangeListener(ChangeEventListener.EventType type, int index) {
        mListener.onChildChanged(type, index, -1);
    }
}
