package com.voxeet.uxkit.implementation;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.voxeet.VoxeetSDK;
import com.voxeet.sdk.exceptions.ExceptionManager;
import com.voxeet.sdk.models.Participant;
import com.voxeet.sdk.utils.Annotate;
import com.voxeet.sdk.utils.NoDocumentation;
import com.voxeet.uxkit.R;
import com.voxeet.uxkit.utils.WindowHelper;
import com.voxeet.uxkit.views.internal.VoxeetVuMeter;
import com.voxeet.uxkit.views.internal.rounded.RoundedImageView;

/**
 * View made to display a given user
 */
@Annotate
public class VoxeetSpeakerView extends VoxeetView {
    private final String TAG = VoxeetSpeakerView.class.getSimpleName();

    public static final int REFRESH_SPEAKER = 1000;

    public static final int REFRESH_METER = 100;

    private Handler handler = new Handler(Looper.getMainLooper());

    private int currentWidth;

    private int orientation = 1;

    private VoxeetVuMeter vuMeter;

    @NonNull
    private RoundedImageView currentSpeakerView;

    private Participant currentSpeaker = null;

    private boolean selected = false;

    private Runnable updateSpeakerRunnable = new Runnable() {
        @Override
        public void run() {
            if (selected && currentSpeaker != null && currentSpeaker.getId() != null) {
                //if we had a user but he disappeared...
                selected = findUserById(currentSpeaker.getId()) != null;
            } else {
                //had a user but predicate did not pass
                selected = false;
            }

            if (!selected && null != VoxeetSDK.conference()) {
                currentSpeaker = findUserById(VoxeetSDK.conference().currentSpeaker());
                if (currentSpeaker != null && currentSpeaker.getInfo() != null) {
                    speakerName.setText(currentSpeaker.getInfo().getName());
                    invalidateSpeakerName();
                }
            }

            if (currentSpeaker != null && currentWidth > 0)
                loadViaPicasso(currentSpeaker, currentWidth / 2, currentSpeakerView);

            if (mAttached) handler.postDelayed(this, REFRESH_SPEAKER);
        }
    };

    private Runnable updateVuMeterRunnable = new Runnable() {
        @Override
        public void run() {
            if (currentSpeaker != null && null != VoxeetSDK.conference()) {
                double value = VoxeetSDK.conference().audioLevel(currentSpeaker);
                vuMeter.updateMeter(value);
            }

            if (mAttached) handler.postDelayed(this, REFRESH_METER);
        }
    };

    private boolean mDisplaySpeakerName = false;
    private TextView speakerName;
    private boolean mAttached;
    private float delta;

    /**
     * Instantiates a new Voxeet current speaker view.
     *
     * @param context the context
     */
    @NoDocumentation
    public VoxeetSpeakerView(Context context) {
        super(context);
    }

    /**
     * Instantiates a new Voxeet current speaker view.
     *
     * @param context the context
     * @param attrs   the attrs
     */
    @NoDocumentation
    public VoxeetSpeakerView(Context context, AttributeSet attrs) {
        super(context, attrs);

        updateAttrs(attrs);
    }

    /**
     * Instantiates a new Voxeet current speaker view.
     *
     * @param context      the context
     * @param attrs        the attrs
     * @param defStyleAttr the def style attr
     */
    @NoDocumentation
    public VoxeetSpeakerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        updateAttrs(attrs);
    }

    @NoDocumentation
    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        mAttached = true;
        onResume();
    }

    @NoDocumentation
    @Override
    protected void onDetachedFromWindow() {
        mAttached = false;
        onPause();

        super.onDetachedFromWindow();
    }

    private void updateAttrs(AttributeSet attrs) {
        delta = WindowHelper.dpToPx(getContext(), 10);

        TypedArray attributes = getContext().obtainStyledAttributes(attrs, R.styleable.VoxeetSpeakerView);
        ColorStateList color = attributes.getColorStateList(R.styleable.VoxeetSpeakerView_vu_meter_color);
        attributes.recycle();

        if (color != null)
            vuMeter.setMeterColor(color.getColorForState(getDrawableState(), 0));
    }

    /**
     * call this method when a conference has been destroyed
     */
    @Override
    public void onConferenceDestroyed() {
        super.onConferenceDestroyed();

        afterLeaving();
    }

    /**
     * Call this method when a conference has been left
     */
    @Override
    public void onConferenceLeft() {
        super.onConferenceLeft();

        afterLeaving();
    }

    private void afterLeaving() {
        currentSpeakerView.setImageDrawable(null);

        vuMeter.reset();

        handler.removeCallbacks(updateSpeakerRunnable);
        handler.removeCallbacks(updateVuMeterRunnable);
    }

    /**
     * Call this method to init this instance of the view
     */
    @Override
    public void init() {
        setShowSpeakerName(true);
    }

    @NoDocumentation
    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        orientation = newConfig.orientation;
        if (orientation <= 0) orientation = 0;
    }

    @NoDocumentation
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        currentWidth = w / orientation;
        int width = currentWidth / 2;

        FrameLayout.LayoutParams paramsMeter = (FrameLayout.LayoutParams) vuMeter.getLayoutParams();
        paramsMeter.gravity = Gravity.CENTER;
        paramsMeter.width = (int) (width + delta);
        paramsMeter.height = (int) (width + delta);
        vuMeter.setLayoutParams(paramsMeter);

        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) currentSpeakerView.getLayoutParams();
        params.gravity = Gravity.CENTER;
        params.width = width;
        params.height = width;
        currentSpeakerView.setLayoutParams(params);
    }

    @NoDocumentation
    @Override
    protected int layout() {
        return R.layout.voxeet_current_speaker_view;
    }

    @NoDocumentation
    @Override
    protected void bindView(View v) {
        currentSpeakerView = v.findViewById(R.id.speaker_image);
        vuMeter = v.findViewById(R.id.vu_meter);
        speakerName = v.findViewById(R.id.speaker_name);
    }

    /**
     * When showing speaker name, it will disable the VuMeter
     *
     * @param display
     */
    @NoDocumentation
    protected void setShowSpeakerName(boolean display) {
        mDisplaySpeakerName = display;

        invalidateSpeakerName();
    }

    private void invalidateSpeakerName() {
        vuMeter.setVisibility(View.VISIBLE);
        speakerName.setVisibility(mDisplaySpeakerName ? View.VISIBLE : View.GONE);
    }

    /**
     * Find user by id conference user.
     *
     * @param userId the user id
     * @return the conference user
     */
    private Participant findUserById(@Nullable final String userId) {
        return VoxeetSDK.conference().findParticipantById(userId);
    }

    private void loadViaPicasso(Participant conferenceUser, int avatarSize, ImageView imageView) {
        try {
            String avatarUrl = null;
            if (null != conferenceUser && null != conferenceUser.getInfo()) {
                avatarUrl = conferenceUser.getInfo().getAvatarUrl();
            }

            if (!TextUtils.isEmpty(avatarUrl)) {
                Picasso.get()
                        .load(conferenceUser.getInfo().getAvatarUrl())
                        .noFade()
                        .resize(avatarSize, avatarSize)
                        .placeholder(R.drawable.default_avatar)
                        .error(R.drawable.default_avatar)
                        .into(imageView);
            } else {
                Picasso.get()
                        .load(R.drawable.default_avatar)
                        .noFade()
                        .resize(avatarSize, avatarSize)
                        .into(imageView);
            }
        } catch (Exception e) {
            ExceptionManager.sendException(e);
            Log.e(TAG, "error " + e.getMessage());
        }
    }

    /**
     * Goes to selected mode and focuses on the user selected instead of updating the speaker view
     * depending on the voice levels.
     *
     * @param user the user to lock onto
     */
    public void lockScreen(@NonNull Participant user) {
        vuMeter.onParticipantSelected();

        currentSpeaker = findUserById(user.getId());

        selected = true;

        String userName = null;
        if (null != currentSpeaker && null != currentSpeaker.getInfo()) {
            userName = currentSpeaker.getInfo().getName();
        }
        if (userName != null) {
            speakerName.setText(userName);
        }
    }

    /**
     * Stops the selected mode.
     */
    public void unlockScreen() {
        vuMeter.onParticipantUnselected();

        onResume();

        selected = false;
    }

    /**
     * Get the selected user for this instance of the view
     *
     * @return the UserId
     */
    @Nullable
    public String getSelectedUserId() {
        return selected && null != currentSpeaker ? currentSpeaker.getId() : null;
    }

    /**
     * Call this method to restore the various callbacks
     */
    @Override
    public void onResume() {
        handler.removeCallbacks(updateSpeakerRunnable);
        handler.removeCallbacks(updateVuMeterRunnable);

        handler.removeCallbacksAndMessages(updateSpeakerRunnable);
        handler.removeCallbacksAndMessages(updateVuMeterRunnable);

        handler.post(updateSpeakerRunnable);
        handler.post(updateVuMeterRunnable);
    }

    /**
     * Call this method to pause the various callbacks
     */
    public void onPause() {
        handler.removeCallbacks(null);
        handler.removeCallbacksAndMessages(null);
    }
}
