package com.mopub.nativeads;

import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.mopub.common.CacheService;

import java.util.List;

import static com.mopub.common.CacheService.DiskLruCacheGetListener;

class ImageDiskTaskManager extends TaskManager<Bitmap> {
    @NonNull private final List<String> mUrls;
    private final int mMaxImageWidth;

    ImageDiskTaskManager(@NonNull final List<String> urls,
            @NonNull final TaskManagerListener<Bitmap> imageTaskManagerListener,
            final int maxImageWidth)
            throws IllegalArgumentException {
        super(urls, imageTaskManagerListener);
        mMaxImageWidth = maxImageWidth;
        mUrls = urls;
    }

    @Override
    void execute() {
        if (mUrls.isEmpty()) {
            mImageTaskManagerListener.onSuccess(mResults);
        }

        ImageDiskTaskListener imageDiskTaskListener = new ImageDiskTaskListener(mMaxImageWidth);
        for (final String url : mUrls) {
            CacheService.getFromDiskCacheAsync(url, imageDiskTaskListener);
        }
    }

    void failAllTasks() {
        if (mFailed.compareAndSet(false, true)) {
            mImageTaskManagerListener.onFail();
        }
    }

    private class ImageDiskTaskListener implements DiskLruCacheGetListener {

        private final int mTargetWidth;

        ImageDiskTaskListener(final int targetWidth) {
            mTargetWidth = targetWidth;
        }

        @Override
        public void onComplete(@Nullable final String key, @Nullable final byte[] content) {
            if (key == null) {
                failAllTasks();
                return;
            } else {
                Bitmap bitmap = null;
                if (content != null) {
                     bitmap = ImageService.byteArrayToBitmap(content, mTargetWidth);
                }
                mResults.put(key, bitmap);
            }

            if (mCompletedCount.incrementAndGet() == mSize) {
                mImageTaskManagerListener.onSuccess(mResults);
            }
        }
    }
}
