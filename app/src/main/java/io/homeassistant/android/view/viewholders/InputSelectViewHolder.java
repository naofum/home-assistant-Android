package io.homeassistant.android.view.viewholders;

import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import java.util.List;

import io.homeassistant.android.HassActivity;
import io.homeassistant.android.R;
import io.homeassistant.android.api.requests.SelectRequest;
import io.homeassistant.android.api.results.Entity;

public class InputSelectViewHolder extends TextViewHolder implements AdapterView.OnItemSelectedListener {

    private final Spinner inputSpinner;
    private int lastSelected;

    public InputSelectViewHolder(View itemView) {
        super(itemView);
        inputSpinner = (Spinner) itemView.findViewById(R.id.input_spinner);
    }

    @Override
    public void setEntity(Entity e) {
        super.setEntity(e);
        inputSpinner.setOnItemSelectedListener(null);
        List<String> options = entity.attributes.options;
        if (options != null) {
            ArrayAdapter adapter = new ArrayAdapter<>(inputSpinner.getContext(), android.support.design.R.layout.support_simple_spinner_dropdown_item, options.toArray());
            inputSpinner.setAdapter(adapter);
            inputSpinner.setSelection(lastSelected = options.indexOf(entity.state));
            inputSpinner.setOnItemSelectedListener(this);
        } else {
            inputSpinner.setVisibility(View.GONE);
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (position == lastSelected) {
            return;
        }
        HassActivity activity = (HassActivity) inputSpinner.getContext();
        activity.send(new SelectRequest(entity, (String) parent.getAdapter().getItem(position)), (success, result) -> {
            if (success) {
                lastSelected = inputSpinner.getSelectedItemPosition();
            } else {
                inputSpinner.setSelection(lastSelected, true);
            }
        });
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        // Never called
    }
}