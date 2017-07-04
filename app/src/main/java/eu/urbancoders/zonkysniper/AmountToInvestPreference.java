package eu.urbancoders.zonkysniper;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.NumberPicker;

import eu.urbancoders.zonkysniper.core.Constants;

/**
 * Selektor pro volbu castky k investovani, pouzito pro rich notifku
 *
 * Author: Ondrej Steger (ondrej@steger.cz)
 * Datum: 04.07.2017
 */

public class AmountToInvestPreference extends DialogPreference {

    private static final String CANCEL = "Odstranit předvolbu";

    public static String[] amountsToInvest = new String[(Constants.AMOUNT_TO_INVEST_MAX / Constants.AMOUNT_TO_INVEST_STEP) + 1];

    static {
        for (int i = Constants.AMOUNT_TO_INVEST_MIN; i <= Constants.AMOUNT_TO_INVEST_MAX; i += Constants.AMOUNT_TO_INVEST_STEP) {
            amountsToInvest[(i/Constants.AMOUNT_TO_INVEST_STEP)-1] = String.valueOf(i);
        }
        amountsToInvest[amountsToInvest.length - 1] = CANCEL;
    }

    // enable or disable the 'circular behavior'
    public static final boolean WRAP_SELECTOR_WHEEL = true;

    private NumberPicker picker;
    private int value;

    public AmountToInvestPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AmountToInvestPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected View onCreateDialogView() {
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.gravity = Gravity.CENTER;

        picker = new NumberPicker(getContext());
        picker.setLayoutParams(layoutParams);
        picker.setDisplayedValues(amountsToInvest);

        FrameLayout dialogView = new FrameLayout(getContext());
        dialogView.addView(picker);

        return dialogView;
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
        picker.setMinValue(0);
        picker.setMaxValue(amountsToInvest.length - 1);
        picker.setWrapSelectorWheel(WRAP_SELECTOR_WHEEL);
        picker.setValue((getValue() / Constants.AMOUNT_TO_INVEST_STEP)-1);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            picker.clearFocus();
            int newValue;
            if(CANCEL.equals(amountsToInvest[picker.getValue()])) {
                newValue = -1;
            } else {
                newValue = Integer.valueOf(amountsToInvest[picker.getValue()]);
            }
            setValue(newValue);
            callChangeListener(newValue);
        }
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getInt(index, Constants.AMOUNT_TO_INVEST_MIN);
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        setValue(restorePersistedValue ? getPersistedInt(Constants.AMOUNT_TO_INVEST_MIN) : (Integer) defaultValue);
    }

    public void setValue(int value) {
        this.value = value;
        persistInt(this.value);
    }

    public int getValue() {
        return this.value;
    }
}