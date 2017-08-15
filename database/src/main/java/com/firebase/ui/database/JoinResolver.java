package com.firebase.ui.database;

import android.support.annotation.NonNull;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;

/**
 * Handles joining two queries together.
 */
public interface JoinResolver {
    /**
     * Called after an {@link ChildEventListener#onChildAdded(DataSnapshot, String)} event from
     * {@code keyRef}.
     *
     * @param keySnapshot the snapshot supplied in {@code onChildAdded}
     * @return A query containing the joined data from the {@code keyRef}'s indexed snapshot.
     * <p>Without any customization, a query on the child from your {@code dataRef} with the key
     * found in {@code keySnapshot} will be returned.
     */
    @NonNull
    DatabaseReference onJoin(DatabaseReference dataRef, DataSnapshot keySnapshot);

    /**
     * Called when a key in {@code keyRef} could not be found in {@code dataRef}.
     *
     * @param snapshot the snapshot who's key could not be found in {@code dataRef}
     * @param index    index of a {@link DataSnapshot} in {@code keyRef} that could not be found in
     *                 {@code dataRef}
     */
    void onJoinFailed(DataSnapshot snapshot, int index);
}
