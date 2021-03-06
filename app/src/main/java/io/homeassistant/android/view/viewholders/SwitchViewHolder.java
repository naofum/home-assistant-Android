package io.homeassistant.android.view.viewholders;

import android.content.res.ColorStateList;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LevelListDrawable;
import android.os.Handler;
import android.support.v7.widget.SwitchCompat;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.widget.SeekBar;

import io.homeassistant.android.Common;
import io.homeassistant.android.HassActivity;
import io.homeassistant.android.R;
import io.homeassistant.android.api.HassUtils;
import io.homeassistant.android.api.requests.ToggleRequest;
import io.homeassistant.android.api.results.Entity;

import static io.homeassistant.android.api.Domain.LIGHT;
import static io.homeassistant.android.api.Domain.SWITCH;

public class SwitchViewHolder extends TextViewHolder implements View.OnTouchListener, View.OnClickListener {

    private final SwitchCompat stateSwitch;
    private final SeekBar brightnessSlider;

    private final Handler handler = new Handler();
    private final Runnable touchDisallowRunnable = () -> itemView.getParent().requestDisallowInterceptTouchEvent(true);
    private final SliderRunnable sliderRunnable = new SliderRunnable();

    public SwitchViewHolder(View itemView) {
        super(itemView);
        stateSwitch = (SwitchCompat) itemView.findViewById(R.id.state_switch);
        stateSwitch.setOnClickListener(null);
        brightnessSlider = (SeekBar) itemView.findViewById(R.id.brightness_slider);
        sliderRunnable.lastEvent = null;
        name.setOnTouchListener(null);
    }

    @Override
    public void setEntity(Entity e) {
        super.setEntity(e);
        stateSwitch.setChecked(entity.state.equals(HassUtils.getOnState(entity, true)));
        stateSwitch.setOnClickListener(this);
        if ((entity.attributes.supported_features & Common.LIGHT_SUPPORTS_BRIGHTNESS) == Common.LIGHT_SUPPORTS_BRIGHTNESS) {
            brightnessSlider.setProgress((int) entity.attributes.brightness);
            name.setOnTouchListener(this);
        }
        updateColor();
    }

    private void updateColor() {
        Drawable leftDrawable = name.getCompoundDrawablesRelative()[0];
        String domain = HassUtils.extractDomainFromEntityId(entity.id);
        if (leftDrawable != null && (domain.equals(LIGHT) || domain.equals(SWITCH))) {
            if (!(leftDrawable instanceof LevelListDrawable)) {
                LevelListDrawable levelListDrawable = new LevelListDrawable();
                // Add states
                levelListDrawable.addLevel(1, 1, leftDrawable);
                BitmapDrawable enabledDrawable = (BitmapDrawable) leftDrawable.getConstantState().newDrawable().mutate();
                enabledDrawable.setTintList(ColorStateList.valueOf(name.getResources().getColor(R.color.color_activated)));
                levelListDrawable.addLevel(2, 2, enabledDrawable);
                // Restore bounds
                levelListDrawable.setBounds(0, 0, name.getResources().getDimensionPixelSize(R.dimen.icon_size), name.getResources().getDimensionPixelSize(R.dimen.icon_size));

                // Set drawable
                name.setCompoundDrawablesRelative(levelListDrawable, null, null, null);
                leftDrawable = levelListDrawable;
            }
            leftDrawable.setLevel(entity.state.equals(HassUtils.getOnState(entity, false)) ? 1 : 2);
        }
    }

    @Override
    public void onClick(View v) {
        HassActivity activity = (HassActivity) v.getContext();
        activity.send(new ToggleRequest(entity, stateSwitch.isChecked()), (success, result) -> {
            if (success) {
                entity.state = HassUtils.getOnState(entity, stateSwitch.isChecked());
                updateColor();
            } else {
                stateSwitch.toggle();
            }
        });
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        brightnessSlider.getLayoutParams().height = name.getHeight();

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                sliderRunnable.previousProgress = brightnessSlider.getProgress();
                sliderRunnable.lastEvent = event;
                sliderRunnable.moved = false;
                handler.postDelayed(touchDisallowRunnable, 200);
                handler.postDelayed(sliderRunnable, 800);
                return true;
            case MotionEvent.ACTION_MOVE:
                sliderRunnable.lastEvent = event;
                if (brightnessSlider.getVisibility() == View.VISIBLE && considerMoved(event)) {
                    brightnessSlider.dispatchTouchEvent(event);
                }
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                handler.removeCallbacks(touchDisallowRunnable);
                handler.removeCallbacks(sliderRunnable);

                if (brightnessSlider.getProgress() != sliderRunnable.previousProgress) { // Changed
                    HassActivity activity = (HassActivity) brightnessSlider.getContext();
                    activity.send(new ToggleRequest(entity, brightnessSlider.getProgress()), (success, result) -> {
                        if (success) {
                            stateSwitch.setChecked(brightnessSlider.getProgress() > 0);
                            entity.state = HassUtils.getOnState(entity, brightnessSlider.getProgress() > 0);
                            updateColor();
                        } else {
                            brightnessSlider.setProgress(sliderRunnable.previousProgress);
                        }
                    });
                }

                brightnessSlider.setVisibility(View.GONE);
                name.setVisibility(View.VISIBLE);
                return true;
            default:
                return false;
        }
    }

    private boolean considerMoved(MotionEvent event) {
        return (sliderRunnable.moved = sliderRunnable.moved || event.getHistorySize() >= 1 && Math.abs(event.getX() - event.getHistoricalX(0)) > 30f);
    }

    private class SliderRunnable implements Runnable {

        int previousProgress;
        MotionEvent lastEvent;
        boolean moved;

        @Override
        public void run() {
            name.setVisibility(View.INVISIBLE);
            brightnessSlider.setVisibility(View.VISIBLE);
            ViewAnimationUtils.createCircularReveal(brightnessSlider, (int) lastEvent.getRawX(), (int) lastEvent.getRawY(), 0, itemView.getWidth())
                    .setDuration(300).start();
        }
    }
}