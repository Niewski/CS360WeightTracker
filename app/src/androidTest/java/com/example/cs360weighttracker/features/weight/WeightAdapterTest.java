package com.example.cs360weighttracker.features.weight;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.appcompat.view.ContextThemeWrapper;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.cs360weighttracker.R;
import com.example.cs360weighttracker.data.WeightEntry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class WeightAdapterTest {

    private Context context;
    private ArrayList<WeightEntry> entries;
    private ViewGroup parent;

    @Before
    public void setUp() {
        context = new ContextThemeWrapper(
                InstrumentationRegistry.getInstrumentation().getTargetContext(),
                R.style.Theme_CS360WeightTracker);
        entries = new ArrayList<>();
        parent = new FrameLayout(context);
    }

    // --- getItemCount: happy path ---

    @Test
    public void getItemCount_withEntries_returnsCorrectCount() {
        entries.add(new WeightEntry(1, "2026-01-15", 165.5));
        entries.add(new WeightEntry(2, "2026-01-16", 164.0));
        WeightAdapter adapter = new WeightAdapter(entries, null, null);

        assertEquals(2, adapter.getItemCount());
    }

    @Test
    public void getItemCount_singleEntry_returnsOne() {
        entries.add(new WeightEntry(1, "2026-01-15", 165.5));
        WeightAdapter adapter = new WeightAdapter(entries, null, null);

        assertEquals(1, adapter.getItemCount());
    }

    // --- getItemCount: edge cases ---

    @Test
    public void getItemCount_emptyList_returnsZero() {
        WeightAdapter adapter = new WeightAdapter(entries, null, null);

        assertEquals(0, adapter.getItemCount());
    }

    @Test
    public void getItemCount_afterExternalAdd_reflectsChange() {
        WeightAdapter adapter = new WeightAdapter(entries, null, null);
        assertEquals(0, adapter.getItemCount());

        entries.add(new WeightEntry(1, "2026-01-15", 165.5));

        assertEquals(1, adapter.getItemCount());
    }

    // --- onCreateViewHolder ---

    @Test
    public void onCreateViewHolder_returnsNonNullViewHolder() {
        WeightAdapter adapter = new WeightAdapter(entries, null, null);

        WeightAdapter.WeightViewHolder holder = adapter.onCreateViewHolder(parent, 0);

        assertNotNull(holder);
        assertNotNull(holder.itemView);
    }

    @Test
    public void onCreateViewHolder_viewHolderHasRequiredViews() {
        WeightAdapter adapter = new WeightAdapter(entries, null, null);

        WeightAdapter.WeightViewHolder holder = adapter.onCreateViewHolder(parent, 0);

        assertNotNull(holder.tvDate);
        assertNotNull(holder.tvWeight);
        assertNotNull(holder.btnDelete);
    }

    // --- onBindViewHolder: happy path ---

    @Test
    public void onBindViewHolder_setsDateText() {
        entries.add(new WeightEntry(1, "2026-01-15", 165.5));
        WeightAdapter adapter = new WeightAdapter(entries, null, null);
        WeightAdapter.WeightViewHolder holder = adapter.onCreateViewHolder(parent, 0);

        adapter.onBindViewHolder(holder, 0);

        assertEquals("2026-01-15", holder.tvDate.getText().toString());
    }

    @Test
    public void onBindViewHolder_setsFormattedWeightText() {
        entries.add(new WeightEntry(1, "2026-01-15", 165.5));
        WeightAdapter adapter = new WeightAdapter(entries, null, null);
        WeightAdapter.WeightViewHolder holder = adapter.onCreateViewHolder(parent, 0);

        adapter.onBindViewHolder(holder, 0);

        assertEquals("165.5 lbs", holder.tvWeight.getText().toString());
    }

    @Test
    public void onBindViewHolder_secondEntry_bindsCorrectData() {
        entries.add(new WeightEntry(1, "2026-01-15", 165.5));
        entries.add(new WeightEntry(2, "2026-01-16", 164.0));
        WeightAdapter adapter = new WeightAdapter(entries, null, null);
        WeightAdapter.WeightViewHolder holder = adapter.onCreateViewHolder(parent, 0);

        adapter.onBindViewHolder(holder, 1);

        assertEquals("2026-01-16", holder.tvDate.getText().toString());
        assertEquals("164.0 lbs", holder.tvWeight.getText().toString());
    }

    // --- onBindViewHolder: delete callback ---

    @Test
    public void onBindViewHolder_deleteClick_invokesListenerWithCorrectId() {
        entries.add(new WeightEntry(42, "2026-01-15", 165.5));
        final int[] deletedId = {-1};
        WeightAdapter adapter = new WeightAdapter(entries, id -> deletedId[0] = id, null);
        WeightAdapter.WeightViewHolder holder = adapter.onCreateViewHolder(parent, 0);

        adapter.onBindViewHolder(holder, 0);
        holder.btnDelete.performClick();

        assertEquals(42, deletedId[0]);
    }

    @Test
    public void onBindViewHolder_deleteClickNullListener_doesNotCrash() {
        entries.add(new WeightEntry(1, "2026-01-15", 165.5));
        WeightAdapter adapter = new WeightAdapter(entries, null, null);
        WeightAdapter.WeightViewHolder holder = adapter.onCreateViewHolder(parent, 0);

        adapter.onBindViewHolder(holder, 0);
        holder.btnDelete.performClick();
        // No exception means pass
    }

    // --- onBindViewHolder: edit callback ---

    @Test
    public void onBindViewHolder_itemClick_invokesEditListenerWithEntry() {
        WeightEntry entry = new WeightEntry(42, "2026-01-15", 165.5);
        entries.add(entry);
        final WeightEntry[] editedEntry = {null};
        WeightAdapter adapter = new WeightAdapter(entries, null, e -> editedEntry[0] = e);
        WeightAdapter.WeightViewHolder holder = adapter.onCreateViewHolder(parent, 0);

        adapter.onBindViewHolder(holder, 0);
        holder.itemView.performClick();

        assertNotNull(editedEntry[0]);
        assertEquals(42, editedEntry[0].id);
        assertEquals("2026-01-15", editedEntry[0].date);
        assertEquals(165.5, editedEntry[0].weight, 0.01);
    }

    @Test
    public void onBindViewHolder_itemClickNullListener_doesNotCrash() {
        entries.add(new WeightEntry(1, "2026-01-15", 165.5));
        WeightAdapter adapter = new WeightAdapter(entries, null, null);
        WeightAdapter.WeightViewHolder holder = adapter.onCreateViewHolder(parent, 0);

        adapter.onBindViewHolder(holder, 0);
        holder.itemView.performClick();
        // No exception means pass
    }

    // --- onBindViewHolder: edge cases ---

    @Test
    public void onBindViewHolder_zeroWeight_formatsCorrectly() {
        entries.add(new WeightEntry(1, "2026-01-15", 0.0));
        WeightAdapter adapter = new WeightAdapter(entries, null, null);
        WeightAdapter.WeightViewHolder holder = adapter.onCreateViewHolder(parent, 0);

        adapter.onBindViewHolder(holder, 0);

        assertEquals("0.0 lbs", holder.tvWeight.getText().toString());
    }

    @Test
    public void onBindViewHolder_largeWeight_formatsCorrectly() {
        entries.add(new WeightEntry(1, "2026-01-15", 999.9));
        WeightAdapter adapter = new WeightAdapter(entries, null, null);
        WeightAdapter.WeightViewHolder holder = adapter.onCreateViewHolder(parent, 0);

        adapter.onBindViewHolder(holder, 0);

        assertEquals("999.9 lbs", holder.tvWeight.getText().toString());
    }

    @Test
    public void onBindViewHolder_weightRounding_formatsToOneDecimal() {
        entries.add(new WeightEntry(1, "2026-01-15", 165.55));
        WeightAdapter adapter = new WeightAdapter(entries, null, null);
        WeightAdapter.WeightViewHolder holder = adapter.onCreateViewHolder(parent, 0);

        adapter.onBindViewHolder(holder, 0);

        assertEquals("165.6 lbs", holder.tvWeight.getText().toString());
    }

    @Test
    public void onBindViewHolder_rebindDifferentEntry_updatesViews() {
        entries.add(new WeightEntry(1, "2026-01-15", 165.5));
        entries.add(new WeightEntry(2, "2026-02-20", 160.0));
        WeightAdapter adapter = new WeightAdapter(entries, null, null);
        WeightAdapter.WeightViewHolder holder = adapter.onCreateViewHolder(parent, 0);

        adapter.onBindViewHolder(holder, 0);
        assertEquals("2026-01-15", holder.tvDate.getText().toString());

        adapter.onBindViewHolder(holder, 1);
        assertEquals("2026-02-20", holder.tvDate.getText().toString());
        assertEquals("160.0 lbs", holder.tvWeight.getText().toString());
    }
}
