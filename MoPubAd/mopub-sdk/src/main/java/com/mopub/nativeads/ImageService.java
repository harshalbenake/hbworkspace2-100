package com.mopub.nativeads;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.Display;
import android.view.WindowManager;

import com.mopub.common.CacheService;
import com.mopub.common.DownloadResponse;
import com.mopub.common.VisibleForTesting;
import com.mopub.common.logging.MoPubLog;
import com.mopub.common.util.VersionCode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.graphics.BitmapFactory.Options;
import static android.graphics.BitmapFactory.decodeByteArray;
import static com.mopub.common.util.VersionCode.HONEYCOMB_MR2;
import static com.mopub.nativeads.TaskManager.TaskManagerListener;
import static java.util.Map.Entry;

class ImageService {
    private static final int TWO_MEGABYTES = 2097152;
    private static int sTargetWidth = -1;

    interface ImageServiceListener {
        void onSuccess(Map<String, Bitmap> bitmaps);
        void onFail();
    }

    @TargetApi(13)
    @VisibleForTesting
    static void initialize(@NonNull Context context) {
        if (sTargetWidth == -1) {
            // Get Display Options
            WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            Display display = wm.getDefaultDisplay();
            Point size = new Point();
            if (VersionCode.currentApiLevel().isBelow(HONEYCOMB_MR2)) {
                size.set(display.getWidth(), display.getHeight());
            } else {
                display.getSize(size);
            }

            // Make our images no wider than the skinny side of the display.
            sTargetWidth = Math.min(size.x, size.y);
        }
    }

    static void get(@NonNull final Context context, @NonNull final List<String> urls,
            @NonNull final ImageServiceListener imageServiceListener) {
        initialize(context);
        CacheService.initialize(context);
        get(urls, imageServiceListener);
    }

    static void get(@NonNull final List<String> urls,
            @NonNull final ImageServiceListener imageServiceListener) {
        final Map<String, Bitmap> cacheBitmaps = new HashMap<String, Bitmap>(urls.size());
        final List<String> urlCacheMisses = getBitmapsFromMemoryCache(urls, cacheBitmaps);

        if (urlCacheMisses.isEmpty()) {
            imageServiceListener.onSuccess(cacheBitmaps);
            return;
        }

        final ImageDiskTaskManager imageDiskTaskManager;
        try {
            imageDiskTaskManager = new ImageDiskTaskManager(
                    urlCacheMisses,
                    new ImageDiskTaskManagerListener(imageServiceListener, cacheBitmaps),
                    sTargetWidth
            );
        } catch (IllegalArgumentException e) {
            MoPubLog.d("Unable to initialize ImageDiskTaskManager", e);
            imageServiceListener.onFail();
            return;
        }

        imageDiskTaskManager.execute();
    }



    static void putBitmapInCache(final String key, final Bitmap bitmap) {
        CacheService.putToBitmapCache(key, bitmap);
    }

    static void putDataInCache(final String key, final Bitmap bitmap, final byte[] byteData) {
        CacheService.putToBitmapCache(key, bitmap);
        CacheService.putToDiskCacheAsync(key, byteData);
    }

    @NonNull
    static List<String> getBitmapsFromMemoryCache(@NonNull final List<String> urls,
            @NonNull final Map<String, Bitmap> hits) {
        final List<String> cacheMisses = new ArrayList<String>();
        for (final String url : urls) {
            final Bitmap bitmap = getBitmapFromMemoryCache(url);

            if (bitmap != null) {
                hits.put(url, bitmap);
            } else {
                cacheMisses.add(url);
            }
        }

        return cacheMisses;
    }

    @Nullable
    static Bitmap getBitmapFromMemoryCache(final String key) {
        return CacheService.getFromBitmapCache(key);
    }

    private static class ImageDiskTaskManagerListener implements TaskManagerListener<Bitmap> {
        final private ImageServiceListener mImageServiceListener;
        final private Map<String, Bitmap> mBitmaps;

        ImageDiskTaskManagerListener(final ImageServiceListener imageServiceListener,
                final Map<String, Bitmap> bitmaps) {
            mImageServiceListener = imageServiceListener;
            mBitmaps = bitmaps;
        }

        @Override
        public void onSuccess(@NonNull final Map<String, Bitmap> diskBitmaps) {
            final List<String> urlDiskMisses = new ArrayList<String>();
            for (final Entry <String, Bitmap> entry : diskBitmaps.entrySet()) {
                if (entry.getValue() == null) {
                    urlDiskMisses.add(entry.getKey());
                } else {
                    putBitmapInCache(entry.getKey(), entry.getValue());
                    mBitmaps.put(entry.getKey(), entry.getValue());
                }
            }

            if (urlDiskMisses.isEmpty()) {
                mImageServiceListener.onSuccess(mBitmaps);
            } else {

                final ImageDownloadTaskManager imageDownloadTaskManager;
                try {
                    imageDownloadTaskManager = new ImageDownloadTaskManager(
                            urlDiskMisses,
                            new ImageDownloadResponseListener(mImageServiceListener, mBitmaps),
                            sTargetWidth
                    );
                } catch (IllegalArgumentException e) {
                    MoPubLog.d("Unable to initialize ImageDownloadTaskManager", e);
                    mImageServiceListener.onFail();
                    return;
                }

                imageDownloadTaskManager.execute();
            }
        }

        @Override
        public void onFail() {
            mImageServiceListener.onFail();
        }
    }

    private static class ImageDownloadResponseListener implements TaskManagerListener<DownloadResponse> {
        private final ImageServiceListener mImageServiceListener;
        private final Map<String, Bitmap> mBitmaps;

        ImageDownloadResponseListener(final ImageServiceListener imageServiceListener,
                final Map<String, Bitmap> bitmaps) {
            mImageServiceListener = imageServiceListener;
            mBitmaps = bitmaps;
        }

        @Override
        public void onSuccess(@NonNull final Map<String, DownloadResponse> responses) {
            for (final Entry<String, DownloadResponse> entry : responses.entrySet()) {
                final Bitmap bitmap = asBitmap(entry.getValue(), sTargetWidth);
                final String key = entry.getKey();
                if (bitmap == null) {
                    MoPubLog.d("Error decoding image for url: " + entry.getKey());
                    onFail();
                    return;
                }

                putDataInCache(key, bitmap, entry.getValue().getByteArray());
                mBitmaps.put(key, bitmap);
            }
            mImageServiceListener.onSuccess(mBitmaps);
        }

        @Override
        public void onFail() {
            mImageServiceListener.onFail();
        }
    }

    @Nullable
    public static Bitmap asBitmap(@NonNull final DownloadResponse downloadResponse, final int requestedWidth) {
        final byte[] bytes = downloadResponse.getByteArray();
        return byteArrayToBitmap(bytes, requestedWidth);
    }

    @Nullable
    public static Bitmap byteArrayToBitmap(@NonNull final byte[] bytes, final int requestedWidth) {
        if (requestedWidth <= 0) {
            return null;
        }

        Options options = new Options();
        options.inJustDecodeBounds = true;
        decodeByteArray(bytes, 0, bytes.length, options);
        options.inSampleSize = calculateInSampleSize(options.outWidth, requestedWidth);

        // If the bitmap will be very large, downsample more to avoid blowing up the heap.
        while (getMemBytes(options) > TWO_MEGABYTES) {
            options.inSampleSize *= 2;
        }

        options.inJustDecodeBounds = false;
        Bitmap bitmap = decodeByteArray(bytes, 0, bytes.length, options);
        if (bitmap == null) {
            return null;
        }

        final int subsampleWidth = bitmap.getWidth();

        // If needed, scale the bitmap so it's exactly the requested width.
        if (subsampleWidth > requestedWidth) {
            final int requestedHeight = (int)(bitmap.getHeight() * (double) requestedWidth / bitmap.getWidth());
            Bitmap subsampledBitmap = bitmap;
            bitmap = Bitmap.createScaledBitmap(subsampledBitmap, requestedWidth, requestedHeight, true);
            subsampledBitmap.recycle();
        }
        
        return bitmap;
    }

    /**
     * Returns the size of the byte array that the bitmap described by the options object will consume.
     */
    public static long getMemBytes(@NonNull Options options) {
        long memBytes = 4 * (long) options.outWidth * (long) options.outHeight / options.inSampleSize / options.inSampleSize;
        return memBytes;
    }

    /**
     * Calculate the largest inSampleSize value that is a power of 2 and keeps the
     * width greater than or equal to the requested width.
     */
    public static int calculateInSampleSize(final int nativeWidth, int requestedWidth) {
        int inSampleSize = 1;

        if (nativeWidth > requestedWidth) {
            final int halfWidth = nativeWidth / 2;

            while ((halfWidth / inSampleSize) >= requestedWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    // Testing, also performs disk IO
    @Nullable
    @Deprecated
    static Bitmap getBitmapFromDiskCache(@NonNull final String key) {
        Bitmap bitmap = null;
        byte[] bytes = CacheService.getFromDiskCache(key);
        if (bytes != null) {
            bitmap = byteArrayToBitmap(bytes, sTargetWidth);
        }
        return bitmap;
    }

    @VisibleForTesting
    static void clear() {
        sTargetWidth = -1;
    }

    @VisibleForTesting
    static int getTargetWidth() {
        return sTargetWidth;
    }
}
