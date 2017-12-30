package com.firebase.ui.firestore

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.OnLifecycleEvent

abstract class SnapshotsHolder<T, in P> : ViewModelBase<P>(), LifecycleObserver {
    lateinit var snapshots: ObservableSnapshotArray<T>
        private set

    override fun onCreate(args: P) {
        snapshots = createSnapshots(args)
        ListenerRegistrationLifecycleOwner.lifecycle.addObserver(this)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    internal fun onActive() {
        snapshots.addChangeEventListener(NoopChangeEventListener)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    internal fun onInactive() {
        snapshots.removeChangeEventListener(NoopChangeEventListener)
    }

    override fun onCleared() {
        super.onCleared()
        ListenerRegistrationLifecycleOwner.lifecycle.removeObserver(this)
        onInactive()
    }

    protected abstract fun createSnapshots(args: P): ObservableSnapshotArray<T>
}
