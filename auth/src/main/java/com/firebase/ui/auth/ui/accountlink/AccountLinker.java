package com.firebase.ui.auth.ui.accountlink;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.util.Log;

import com.firebase.ui.auth.ErrorCodes;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.ResultCodes;
import com.firebase.ui.auth.ui.BaseHelper;
import com.firebase.ui.auth.ui.TaskFailureLogger;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;

/**
 * "One link to rule them all." - AccountLinker
 * <p><p>
 * AccountLinker can handle up to 3 way account linking: user is currently logged in anonymously,
 * has an existing Google account, and is trying to log in with Facebook.
 * Results: Google and Facebook are linked and the uid of the anonymous account is returned for manual merging.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class AccountLinker implements OnSuccessListener<AuthResult>, OnFailureListener {
    private static final String TAG = "AccountLinker";

    private Activity mActivity;
    private BaseHelper mHelper;
    private IdpResponse mResponse;

    /**
     * The credential of the user's existing account.
     */
    private AuthCredential mExistingCredential;

    /**
     * The credential the user originally tried to sign in with.
     */
    private AuthCredential mNewCredential;

    private AccountLinker(Activity activity,
                          BaseHelper helper,
                          IdpResponse response,
                          AuthCredential existingCredential,
                          AuthCredential newCredential) {
        mActivity = activity;
        mHelper = helper;
        mResponse = response;
        mExistingCredential = existingCredential;
        mNewCredential = newCredential;
        start();
    }

    public static void link(Activity activity,
                            BaseHelper helper,
                            IdpResponse response,
                            @NonNull AuthCredential existingCredential,
                            @Nullable AuthCredential newCredential) {
        new AccountLinker(activity, helper, response, existingCredential, newCredential);
    }

    private void start() {
        FirebaseUser currentUser = mHelper.getCurrentUser();
        if (currentUser == null) {
            // The user has an existing account and is trying to log in with a new provider
            mHelper.getFirebaseAuth()
                    .signInWithCredential(mExistingCredential)
                    .addOnSuccessListener(this)
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            finishWithError();
                        }
                    })
                    .addOnFailureListener(
                            new TaskFailureLogger(TAG, "Error signing in with new credential"));
        } else {
            // If the current user is trying to sign in with a new account, just link the two
            // and we should be fine. Otherwise, we are probably working with an anonymous account
            // trying to be linked to an existing account which is bound to fail.
            currentUser
                    .linkWithCredential(mNewCredential == null ? mExistingCredential : mNewCredential)
                    .addOnSuccessListener(new FinishListener())
                    .addOnFailureListener(this)
                    .addOnFailureListener(
                            new TaskFailureLogger(TAG, "Error linking with credential"));
        }
    }

    @Override
    public void onSuccess(AuthResult result) {
        if (mNewCredential == null) {
            mHelper.finishActivity(mActivity, ResultCodes.OK, IdpResponse.getIntent(mResponse));
        } else {
            // Link the user's existing account (mExistingCredential) with the account they were
            // trying to sign in to (mNewCredential)
            result.getUser()
                    .linkWithCredential(mNewCredential)
                    .addOnFailureListener(new TaskFailureLogger(
                            TAG, "Error signing in with previous credential"))
                    .addOnCompleteListener(new FinishListener());
        }
    }

    @Override
    public void onFailure(@NonNull Exception e) {
        if (e instanceof FirebaseAuthUserCollisionException && mHelper.canLinkAccounts()) {
            mResponse.setPrevUid(mHelper.getUidForAccountLinking());

            // Since we still want the user to be able to sign in even though
            // they have an existing account, we are going to save the uid of the
            // current user, log them out, and then sign in with the new credential.
            Task<AuthResult> signInTask = mHelper.getFirebaseAuth()
                    .signInWithCredential(mExistingCredential)
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            finishWithError();
                        }
                    })
                    .addOnFailureListener(
                            new TaskFailureLogger(TAG, "Error signing in with credential"));

            // Occurs when the user is logged and they are trying to sign in with an existing account.
            if (mNewCredential == null) {
                signInTask.addOnSuccessListener(new FinishListener());
            } else {
                // 3 way account linking!!!
                // Occurs if the user is logged, trying to sign in with a new provider,
                // and already has existing providers.
                signInTask.addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult result) {
                        mHelper.getCurrentUser()
                                .linkWithCredential(mNewCredential)
                                .addOnFailureListener(
                                        new TaskFailureLogger(TAG, "Error linking with credential"))
                                .addOnCompleteListener(new FinishListener());
                    }
                });
            }
        } else {
            Log.w(TAG, "See AuthUI.SignInIntentBuilder#setShouldLinkAccounts(boolean) to support account linking");
            finishWithError();
        }
    }

    private void finishWithError() {
        mHelper.finishActivity(
                mActivity,
                ResultCodes.CANCELED,
                IdpResponse.getErrorCodeIntent(ErrorCodes.UNKNOWN_ERROR));
    }

    private class FinishListener implements OnCompleteListener<AuthResult>, OnSuccessListener<AuthResult> {
        @Override
        public void onComplete(@NonNull Task task) {
            finishOk();
        }

        @Override
        public void onSuccess(AuthResult result) {
            finishOk();
        }

        private void finishOk() {
            mHelper.finishActivity(mActivity, ResultCodes.OK, IdpResponse.getIntent(mResponse));
        }
    }
}
