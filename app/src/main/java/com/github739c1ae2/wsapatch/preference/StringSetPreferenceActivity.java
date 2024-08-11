package com.github739c1ae2.wsapatch.preference;

import android.content.Intent;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.github739c1ae2.wsapatch.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class StringSetPreferenceActivity extends ActivityPreferenceActivity {

    private Set<String> mStringSet;
    private StringListAdapter mAdapter;
    private List<String> mItems;

    @Override
    @SuppressWarnings("unchecked")
    protected void onGetDefaultValue(@NonNull Intent intent) {
        Set<String> defaultValue = intent.getSerializableExtra(EXTRA_DEFAULT_VALUE, HashSet.class);
        mStringSet = getSharedPreferences().getStringSet(getKey(),
                defaultValue != null ? defaultValue : new HashSet<>());
    }

    @Override
    protected void onInitialize() {
        setContentView(R.layout.activity_string_set_preference);
        if (!TextUtils.isEmpty(getSummary())) {
            ((TextView) findViewById(R.id.tv_summary)).setText(getSummary());
        }
        RecyclerView recyclerView = findViewById(R.id.rv_list);
        FloatingActionButton addButton = findViewById(R.id.fab_add);
        mItems = new ArrayList<>(mStringSet);
        mAdapter = new StringListAdapter();
        recyclerView.setAdapter(mAdapter);
        addButton.setOnClickListener(v -> showAddItemDialog());
    }

    private void showAddItemDialog() {
        View view = getLayoutInflater().inflate(R.layout.preference_edit_text, null);
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.add_item)
                .setView(view)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    EditText editText = view.findViewById(android.R.id.edit);
                    String item = editText.getText().toString();
                    if (item.isEmpty()) {
                        return;
                    }
                    if (mStringSet.contains(item)) {
                        Toast.makeText(StringSetPreferenceActivity.this,
                                R.string.item_already_exists, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    mStringSet.add(item);
                    getEditor().putStringSet(getKey(), mStringSet).apply();
                    mItems.add(item);
                    mAdapter.notifyItemInserted(mItems.size() - 1);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void showEditItemDialog(int position) {
        View view = getLayoutInflater().inflate(R.layout.preference_edit_text, null);
        EditText editText = view.findViewById(android.R.id.edit);
        editText.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
        editText.setText(mItems.get(position));
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.edit_item)
                .setView(view)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    String item = editText.getText().toString();
                    if (item.isEmpty()) {
                        return;
                    }
                    if (mStringSet.contains(item)) {
                        Toast.makeText(StringSetPreferenceActivity.this,
                                R.string.item_already_exists, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    mStringSet.remove(mItems.get(position));
                    mStringSet.add(item);
                    getEditor().putStringSet(getKey(), mStringSet).apply();
                    mItems.set(position, item);
                    mAdapter.notifyItemChanged(position);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .setNeutralButton(R.string.delete, (dialog, which) -> {
                    mStringSet.remove(mItems.get(position));
                    getEditor().putStringSet(getKey(), mStringSet).apply();
                    mItems.remove(position);
                    mAdapter.notifyItemRemoved(position);
                })
                .show();
    }

    private class StringListAdapter extends RecyclerView.Adapter<StringListAdapter.ItemViewHolder> {

        @NonNull
        @Override
        public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.list_item_string, parent, false);
            return new ItemViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
            String item = mItems.get(position);
            holder.bind(item);
        }

        @Override
        public int getItemCount() {
            return mItems.size();
        }

        class ItemViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
            private final TextView textView;

            public ItemViewHolder(@NonNull View itemView) {
                super(itemView);
                textView = itemView.findViewById(android.R.id.text1);
                itemView.setOnClickListener(this);
            }

            public void bind(@NonNull String item) {
                textView.setText(item);
            }

            @Override
            public void onClick(View v) {
                showEditItemDialog(getAdapterPosition());
            }
        }
    }

}