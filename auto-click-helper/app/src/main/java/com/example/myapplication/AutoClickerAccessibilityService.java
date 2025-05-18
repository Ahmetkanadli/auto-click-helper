package com.example.myapplication;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.Build;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import androidx.annotation.RequiresApi;

public class AutoClickerAccessibilityService extends AccessibilityService {
    
    private static final String TAG = "AutoClickerService";
    private static final String TARGET_BUTTON_TEXT = "ONAYLA";
    private static final String ALERT_TITLE_TEXT = "Uyarı";
    private static final String CONFIRMATION_TEXT = "Bir sonraki eğitime geçilecek onaylıyor musunuz?";

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || 
            event.getEventType() == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            
            // Get the root node
            AccessibilityNodeInfo rootNode = getRootInActiveWindow();
            if (rootNode == null) {
                return;
            }

            // Check for alert dialog
            boolean isAlertDialog = searchForText(rootNode, ALERT_TITLE_TEXT);
            boolean hasConfirmationText = searchForText(rootNode, CONFIRMATION_TEXT);
            
            if (isAlertDialog || hasConfirmationText) {
                Log.d(TAG, "Found alert dialog");
                // Try to find the ONAYLA button by text
                AccessibilityNodeInfo button = findButtonByText(rootNode, TARGET_BUTTON_TEXT);
                
                if (button != null) {
                    Log.d(TAG, "Found ONAYLA button, attempting to click");
                    performButtonClick(button);
                    button.recycle();
                } else {
                    // Try to find the button by looking at the bottom right
                    findAndClickRightButton(rootNode);
                }
            }
            
            rootNode.recycle();
        }
    }

    private boolean searchForText(AccessibilityNodeInfo node, String text) {
        if (node == null) return false;
        
        if (node.getText() != null && node.getText().toString().contains(text)) {
            return true;
        }
        
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                if (searchForText(child, text)) {
                    child.recycle();
                    return true;
                }
                child.recycle();
            }
        }
        
        return false;
    }

    private AccessibilityNodeInfo findButtonByText(AccessibilityNodeInfo node, String buttonText) {
        if (node == null) return null;
        
        // Check if this node is a button with the target text
        if (node.getText() != null && 
            node.getText().toString().equalsIgnoreCase(buttonText) && 
            node.isClickable()) {
            return AccessibilityNodeInfo.obtain(node);
        }
        
        // Check child nodes
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                AccessibilityNodeInfo result = findButtonByText(child, buttonText);
                child.recycle();
                if (result != null) {
                    return result;
                }
            }
        }
        
        return null;
    }

    private void findAndClickRightButton(AccessibilityNodeInfo rootNode) {
        // Try to find buttons at the bottom of the screen
        AccessibilityNodeInfo rightButton = null;
        int maxX = -1;
        
        for (int i = 0; i < rootNode.getChildCount(); i++) {
            AccessibilityNodeInfo child = rootNode.getChild(i);
            if (child != null) {
                if (child.isClickable()) {
                    Rect bounds = new Rect();
                    child.getBoundsInScreen(bounds);
                    
                    // Look for rightmost button (ONAYLA is typically on the right)
                    if (bounds.right > maxX) {
                        if (rightButton != null) {
                            rightButton.recycle();
                        }
                        rightButton = AccessibilityNodeInfo.obtain(child);
                        maxX = bounds.right;
                    }
                }
                
                // Also search among child's children
                AccessibilityNodeInfo deepResult = findRightmostClickableNode(child);
                child.recycle();
                
                if (deepResult != null) {
                    Rect bounds = new Rect();
                    deepResult.getBoundsInScreen(bounds);
                    
                    if (bounds.right > maxX) {
                        if (rightButton != null) {
                            rightButton.recycle();
                        }
                        rightButton = deepResult;
                        maxX = bounds.right;
                    } else {
                        deepResult.recycle();
                    }
                }
            }
        }
        
        if (rightButton != null) {
            Log.d(TAG, "Found rightmost button, attempting to click");
            performButtonClick(rightButton);
            rightButton.recycle();
        }
    }

    private AccessibilityNodeInfo findRightmostClickableNode(AccessibilityNodeInfo node) {
        if (node == null) return null;
        
        AccessibilityNodeInfo result = null;
        int maxX = -1;
        
        // Check if this node is clickable
        if (node.isClickable()) {
            Rect bounds = new Rect();
            node.getBoundsInScreen(bounds);
            result = AccessibilityNodeInfo.obtain(node);
            maxX = bounds.right;
        }
        
        // Check child nodes
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                AccessibilityNodeInfo childResult = findRightmostClickableNode(child);
                child.recycle();
                
                if (childResult != null) {
                    Rect childBounds = new Rect();
                    childResult.getBoundsInScreen(childBounds);
                    
                    if (childBounds.right > maxX) {
                        if (result != null) {
                            result.recycle();
                        }
                        result = childResult;
                        maxX = childBounds.right;
                    } else {
                        childResult.recycle();
                    }
                }
            }
        }
        
        return result;
    }

    private void performButtonClick(AccessibilityNodeInfo button) {
        if (button == null) return;
        
        // First try standard click action
        if (button.isClickable()) {
            button.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            Log.d(TAG, "Performed standard click");
            return;
        }
        
        // If standard click doesn't work, try a gesture tap for API 24+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            clickByGesture(button);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void clickByGesture(AccessibilityNodeInfo node) {
        Rect rect = new Rect();
        node.getBoundsInScreen(rect);
        
        int x = (rect.left + rect.right) / 2;
        int y = (rect.top + rect.bottom) / 2;
        
        Path path = new Path();
        path.moveTo(x, y);
        
        GestureDescription.Builder builder = new GestureDescription.Builder();
        builder.addStroke(new GestureDescription.StrokeDescription(path, 0, 50));
        
        dispatchGesture(builder.build(), new GestureResultCallback() {
            @Override
            public void onCompleted(GestureDescription gestureDescription) {
                super.onCompleted(gestureDescription);
                Log.d(TAG, "Gesture tap completed at x=" + x + ", y=" + y);
            }
            
            @Override
            public void onCancelled(GestureDescription gestureDescription) {
                super.onCancelled(gestureDescription);
                Log.d(TAG, "Gesture tap cancelled");
            }
        }, null);
    }

    @Override
    public void onInterrupt() {
        Log.d(TAG, "Accessibility service interrupted");
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        Log.d(TAG, "Accessibility service connected");
    }
} 