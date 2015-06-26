package com.mopub.mobileads.util.vast;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class VastVideoConfiguration implements Serializable {
    private static final long serialVersionUID = 0L;

    private ArrayList<String> mImpressionTrackers;
    private ArrayList<String> mStartTrackers;
    private ArrayList<String> mFirstQuartileTrackers;
    private ArrayList<String> mMidpointTrackers;
    private ArrayList<String> mThirdQuartileTrackers;
    private ArrayList<String> mCompleteTrackers;
    private ArrayList<String> mClickTrackers;
    private String mClickThroughUrl;
    private String mNetworkMediaFileUrl;
    private String mDiskMediaFileUrl;
    private VastCompanionAd mVastCompanionAd;

    public VastVideoConfiguration() {
        mImpressionTrackers = new ArrayList<String>();
        mStartTrackers = new ArrayList<String>();
        mFirstQuartileTrackers = new ArrayList<String>();
        mMidpointTrackers = new ArrayList<String>();
        mThirdQuartileTrackers = new ArrayList<String>();
        mCompleteTrackers = new ArrayList<String>();
        mClickTrackers = new ArrayList<String>();
    }

    /**
     * Setters
     */

    public void addImpressionTrackers(final List<String> impressionTrackers) {
        mImpressionTrackers.addAll(impressionTrackers);
    }

    public void addStartTrackers(final List<String> startTrackers) {
        mStartTrackers.addAll(startTrackers);
    }

    public void addFirstQuartileTrackers(final List<String> firstQuartileTrackers) {
        mFirstQuartileTrackers.addAll(firstQuartileTrackers);
    }

    public void addMidpointTrackers(final List<String> midpointTrackers) {
        mMidpointTrackers.addAll(midpointTrackers);
    }

    public void addThirdQuartileTrackers(final List<String> thirdQuartileTrackers) {
        mThirdQuartileTrackers.addAll(thirdQuartileTrackers);
    }

    public void addCompleteTrackers(final List<String> completeTrackers) {
        mCompleteTrackers.addAll(completeTrackers);
    }

    public void addClickTrackers(final List<String> clickTrackers) {
        mClickTrackers.addAll(clickTrackers);
    }

    public void setClickThroughUrl(final String clickThroughUrl) {
        mClickThroughUrl = clickThroughUrl;
    }

    public void setNetworkMediaFileUrl(final String networkMediaFileUrl) {
        mNetworkMediaFileUrl = networkMediaFileUrl;
    }

    public void setDiskMediaFileUrl(final String diskMediaFileUrl) {
        mDiskMediaFileUrl = diskMediaFileUrl;
    }

    public void setVastCompanionAd(final VastCompanionAd vastCompanionAd) {
        mVastCompanionAd = vastCompanionAd;
    }

    /**
     * Getters
     */

    public List<String> getImpressionTrackers() {
        return mImpressionTrackers;
    }

    public List<String> getStartTrackers() {
        return mStartTrackers;
    }

    public List<String> getFirstQuartileTrackers() {
        return mFirstQuartileTrackers;
    }

    public List<String> getMidpointTrackers() {
        return mMidpointTrackers;
    }

    public List<String> getThirdQuartileTrackers() {
        return mThirdQuartileTrackers;
    }

    public List<String> getCompleteTrackers() {
        return mCompleteTrackers;
    }

    public List<String> getClickTrackers() {
        return mClickTrackers;
    }

    public String getClickThroughUrl() {
        return mClickThroughUrl;
    }

    public String getNetworkMediaFileUrl() {
        return mNetworkMediaFileUrl;
    }

    public String getDiskMediaFileUrl() {
        return mDiskMediaFileUrl;
    }

    public VastCompanionAd getVastCompanionAd() {
        return mVastCompanionAd;
    }
}
