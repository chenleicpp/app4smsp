package com.sjz.cl.smspopup.ui;

import java.util.ArrayList;
import java.util.List;

import uk.co.jasonfry.android.tools.ui.SwipeView;

import com.sjz.cl.smspopup.ui.SmsbodyView.OnButtonClicked;
import com.sjz.cl.smspopup.utils.MessageObject;


import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;

public class SmsPopupSwipeView extends SwipeView {

    private static final String TAG = "SmsPopupSwipeView";

    private Context context;
    private ArrayList<MessageObject> messages;
    private MessageCountChanged messageCountChanged;

    public SmsPopupSwipeView(Context context) {
        super(context);
        init(context);
    }

    public SmsPopupSwipeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context c) {
        context = c;
        messages = new ArrayList<MessageObject>(5);
        setOnPageChangedListener(new OnPageChangedListener() {
            @Override
            public void onPageChanged(int oldPage, int newPage) {
                UpdateMessageCount();
            }
        });
    }

    /**
     * Add a message and its view to the end of the list of messages
     * 
     * @param newMessage
     */
    public void addMessage(OnButtonClicked obc, MessageObject newMessage) {
        messages.add(newMessage);
        SmsbodyView sv = new SmsbodyView(context, newMessage);
        addView(sv);
        sv.setOnButtonClicked(obc);
        UpdateMessageCount();
    }

    public void addMessages(ArrayList<MessageObject> newMessages) {
        if (newMessages != null) {
            for (int i = 0; i < newMessages.size(); i++) {
                addView(new SmsbodyView(context, newMessages.get(i)));
            }
            messages.addAll(newMessages);
            UpdateMessageCount();
        }
    }

    /**
     * Remove the message and its view and the location numMessage
     * 
     * @param numMessage
     * @return true if there were no more messages to remove, false otherwise
     */
    public boolean removeMessage(int numMessage) {
        final int totalMessages = getPageCount();
        final int currentMessage = getCurrentPage();

        if (numMessage < totalMessages && numMessage >= 0 && totalMessages > 1) {
            if (currentMessage == numMessage) {
                // If removing last page, go to previous
                if (currentMessage == (totalMessages - 1)) {
                    showPrevious();
                } else {
                    // showNext();
                }
            }

            // Remove the view
            removeView(numMessage);

            // Remove message from arraylist
            messages.remove(numMessage);

            // Run any other updates (as set by interface)
            UpdateMessageCount();

            // If more messages, return false
            if (totalMessages > 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Remove the currently active message, if there is only one message left
     * then it will not be removed
     * 
     * @return true if there were no more messages to remove, false otherwise
     */
    public boolean removeActiveMessage() {
        return removeMessage(getCurrentPage());
    }

    /**
     * Removes a view from the child layout
     * 
     * @param index
     *            view to remove
     */
    public void removeView(final int index) {
        getChildContainer().removeViewAt(index);
    }

    /**
     * Return the currently active message
     * 
     * @return
     */
    public MessageObject getActiveMessage() {
        return messages.get(getCurrentPage());
    }

    public List<MessageObject> getMessages(){
        return messages;
    }
    
    public void setOnMessageCountChanged(MessageCountChanged m) {
        messageCountChanged = m;
    }

    public static interface MessageCountChanged {
        abstract void onChange(int current, int total);
    }

    private void UpdateMessageCount() {
        if (messageCountChanged != null) {
            messageCountChanged.onChange(getCurrentPage(), getPageCount());
        }
    }

    public void showNext() {
        Log.d(TAG, " showNext() - " + getCurrentPage() + ", " + getActiveMessage().getFromAddress());
        smoothScrollToPage(getCurrentPage() + 1);
    }

    public void showPrevious() {
        Log.d(TAG, "showPrevious() - " + getCurrentPage() + ", " + getActiveMessage().getFromAddress());
        smoothScrollToPage(getCurrentPage() - 1);
    }

}