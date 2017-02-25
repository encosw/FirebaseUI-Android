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
import android.support.annotation.RestrictTo;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * This class implements a collection on top of a Firebase location.
 */
public class FirebaseArray<T> extends ObservableSnapshotArray<T> implements ChildEventListener, ValueEventListener {

    protected final Query mQuery;
    private List<DataSnapshot> mSnapshots = new ArrayList<>();

    /**
     * @param query The Firebase location to watch for data changes. Can also be a slice of a
     *              location, using some combination of {@code limit()}, {@code startAt()}, and
     *              {@code endAt()}.
     */
    public FirebaseArray(Query query) {
        this(query, null);
    }

    public FirebaseArray(Query query, SnapshotParser<T> parser) {
        super(parser);
        mQuery = query;
    }

    @Override
    public T getObject(int index) {
        // TODO: Cache this!
        return super.getObject(index);
    }

    @Override
    public ChangeEventListener addChangeEventListener(@NonNull ChangeEventListener listener) {
        boolean wasListening = isListening();
        super.addChangeEventListener(listener);

        // Only start listening when the first listener is added
        if (!wasListening) {
            mQuery.addChildEventListener(this);
            mQuery.addValueEventListener(this);
        }

        return listener;
    }

    @Override
    public void removeChangeEventListener(@NonNull ChangeEventListener listener) {
        super.removeChangeEventListener(listener);

        // Clear data when all listeners are removed
        if (!isListening()) {
            mQuery.removeEventListener((ValueEventListener) this);
            mQuery.removeEventListener((ChildEventListener) this);

            mSnapshots.clear();
        }
    }

    @Override
    public void onChildAdded(DataSnapshot snapshot, String previousChildKey) {
        int index = 0;
        if (previousChildKey != null) {
            index = getIndexForKey(previousChildKey) + 1;
        }

        mSnapshots.add(index, snapshot);

        notifyChangeEventListeners(ChangeEventListener.EventType.ADDED, snapshot, index);
    }

    @Override
    public void onChildChanged(DataSnapshot snapshot, String previousChildKey) {
        int index = getIndexForKey(snapshot.getKey());

        mSnapshots.set(index, snapshot);

        notifyChangeEventListeners(ChangeEventListener.EventType.CHANGED, snapshot, index);
    }

    @Override
    public void onChildRemoved(DataSnapshot snapshot) {
        int index = getIndexForKey(snapshot.getKey());

        mSnapshots.remove(index);

        notifyChangeEventListeners(ChangeEventListener.EventType.REMOVED, snapshot, index);
    }

    @Override
    public void onChildMoved(DataSnapshot snapshot, String previousChildKey) {
        int oldIndex = getIndexForKey(snapshot.getKey());
        mSnapshots.remove(oldIndex);

        int newIndex = previousChildKey == null ? 0 : (getIndexForKey(previousChildKey) + 1);
        mSnapshots.add(newIndex, snapshot);

        notifyChangeEventListeners(ChangeEventListener.EventType.MOVED, snapshot,
                                   newIndex, oldIndex);
    }

    @Override
    public void onDataChange(DataSnapshot dataSnapshot) {
        notifyListenersOnDataChanged();
    }

    @Override
    public void onCancelled(DatabaseError error) {
        notifyListenersOnCancelled(error);
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

    public DataSnapshot getSnapshot(int index) {
        return mSnapshots.get(index);
    }

    @Override
    public int size() {
        return mSnapshots.size();
    }

    @Override
    public boolean isEmpty() {
        return mSnapshots.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return mSnapshots.contains(o);
    }

    /**
     * {@inheritDoc}
     *
     * @return an immutable iterator
     */
    @Override
    public Iterator<DataSnapshot> iterator() {
        return new ImmutableIterator(mSnapshots.iterator());
    }

    @Override
    public DataSnapshot[] toArray() {
        return mSnapshots.toArray(new DataSnapshot[mSnapshots.size()]);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return mSnapshots.containsAll(c);
    }

    @Override
    public DataSnapshot get(int index) {
        return mSnapshots.get(index);
    }

    @Override
    public int indexOf(Object o) {
        return mSnapshots.indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o) {
        return mSnapshots.lastIndexOf(o);
    }

    /**
     * {@inheritDoc}
     *
     * @return an immutable list iterator
     */
    @Override
    public ListIterator<DataSnapshot> listIterator() {
        return new ImmutableListIterator(mSnapshots.listIterator());
    }

    /**
     * {@inheritDoc}
     *
     * @return an immutable list iterator
     */
    @Override
    public ListIterator<DataSnapshot> listIterator(int index) {
        return new ImmutableListIterator(mSnapshots.listIterator(index));
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        FirebaseArray snapshots = (FirebaseArray) obj;

        return mQuery.equals(snapshots.mQuery) && mSnapshots.equals(snapshots.mSnapshots);
    }

    @Override
    public int hashCode() {
        int result = mQuery.hashCode();
        result = 31 * result + mSnapshots.hashCode();
        return result;
    }

    @Override
    public String toString() {
        if (isListening()) {
            return "FirebaseArray is listening at " + mQuery + ":\n" + mSnapshots;
        } else {
            return "FirebaseArray is inactive";
        }
    }

    /**
     * Guaranteed to throw an exception. Use {@link #toArray()} instead to get an array of {@link
     * DataSnapshot}s.
     *
     * @throws UnsupportedOperationException always
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Override
    public final <T> T[] toArray(T[] a) {
        throw new UnsupportedOperationException();
    }
}
