/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.documentsui.services;

import static com.android.documentsui.StubProvider.ROOT_0_ID;
import static com.android.documentsui.StubProvider.ROOT_1_ID;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.RemoteException;
import android.test.AndroidTestCase;

import androidx.test.InstrumentationRegistry;
import androidx.test.filters.MediumTest;

import com.android.documentsui.DocumentsProviderHelper;
import com.android.documentsui.R;
import com.android.documentsui.StubProvider;
import com.android.documentsui.base.DocumentInfo;
import com.android.documentsui.base.DocumentStack;
import com.android.documentsui.base.RootInfo;
import com.android.documentsui.base.UserId;
import com.android.documentsui.clipping.UrisSupplier;
import com.android.documentsui.services.FileOperationService.OpType;
import com.android.documentsui.testing.DocsProviders;
import com.android.documentsui.testing.TestFeatures;

import java.util.List;

@MediumTest
public abstract class AbstractJobTest<T extends Job> extends AndroidTestCase {

    static final String AUTHORITY = StubProvider.DEFAULT_AUTHORITY;
    static final byte[] HAM_BYTES = "ham and cheese".getBytes();
    static final byte[] FRUITY_BYTES = "I love fruit cakes!".getBytes();

    UserId mUserId;
    Context mContext;
    ContentResolver mResolver;
    DocumentsProviderHelper mDocs;
    TestJobListener mJobListener;
    RootInfo mSrcRoot;
    RootInfo mDestRoot;

    private TestFeatures mFeatures;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        mFeatures = new TestFeatures();
        mFeatures.notificationChannel = InstrumentationRegistry.getTargetContext()
                .getResources().getBoolean(R.bool.feature_notification_channel);

        mJobListener = new TestJobListener();

        // NOTE: Must be the "target" context, else security checks in content provider will fail.
        mUserId = UserId.DEFAULT_USER;
        mContext = getContext();
        mResolver = mContext.getContentResolver();

        mDocs = new DocumentsProviderHelper(mUserId, AUTHORITY, mContext, AUTHORITY);

        // Reset storage before starting the tests.
        resetStorage();

        initTestFiles();
    }

    @Override
    protected void tearDown() throws Exception {
        resetStorage();
        mDocs.cleanUp();
        super.tearDown();
    }

    private void resetStorage() throws RemoteException {
        mDocs.clear(null, null);
    }

    private void initTestFiles() throws RemoteException {
        mSrcRoot = mDocs.getRoot(ROOT_0_ID);
        mDestRoot = mDocs.getRoot(ROOT_1_ID);
    }

    FileOperation createOperation(@OpType int opType, List<Uri> srcs, Uri srcParent,
            Uri destination) throws Exception {
        DocumentStack stack =
                new DocumentStack(mSrcRoot, DocumentInfo.fromUri(mResolver, destination, mUserId));

        UrisSupplier urisSupplier = DocsProviders.createDocsProvider(srcs);
        FileOperation operation = new FileOperation.Builder()
                .withOpType(opType)
                .withSrcs(urisSupplier)
                .withDestination(stack)
                .withSrcParent(srcParent)
                .build();
        return operation;
    }

    final T createJob(FileOperation operation) {
        return createJob(operation, mJobListener);
    }

    final T createJob(FileOperation operation, Job.Listener listener) {
        return (T) operation.createJob(
                mContext, listener, FileOperations.createJobId(), mFeatures);
    }

    final T createJob(@OpType int opType, List<Uri> srcs, Uri srcParent, Uri destination)
            throws Exception {
        DocumentStack stack =
                new DocumentStack(mSrcRoot, DocumentInfo.fromUri(mResolver, destination, mUserId));

        UrisSupplier urisSupplier = DocsProviders.createDocsProvider(srcs);
        FileOperation operation = new FileOperation.Builder()
                .withOpType(opType)
                .withSrcs(urisSupplier)
                .withDestination(stack)
                .withSrcParent(srcParent)
                .build();
        return (T) operation.createJob(
                mContext, mJobListener, FileOperations.createJobId(), mFeatures);
    }
}
