package com.kulenina.questix.viewmodel;

import androidx.databinding.BaseObservable;
import androidx.databinding.Bindable;

import com.kulenina.questix.BR;

public class QRScannerViewModel extends BaseObservable {
    private boolean isLoading = false;
    private String statusMessage = null;
    private String loadingMessage = "Processing...";

    @Bindable
    public boolean getIsLoading() {
        return isLoading;
    }

    public void setIsLoading(boolean isLoading) {
        this.isLoading = isLoading;
        notifyPropertyChanged(BR.isLoading);
    }

    @Bindable
    public String getStatusMessage() {
        return statusMessage;
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
        notifyPropertyChanged(BR.statusMessage);
    }

    @Bindable
    public String getLoadingMessage() {
        return loadingMessage;
    }

    public void setLoadingMessage(String loadingMessage) {
        this.loadingMessage = loadingMessage;
        notifyPropertyChanged(BR.loadingMessage);
    }
}
