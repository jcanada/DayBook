package com.example.joanericacanada.daybook.view;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import com.example.joanericacanada.daybook.R;
import com.example.joanericacanada.daybook.controller.EntryKeeper;
import com.example.joanericacanada.daybook.controller.JournalAdapter;
import com.example.joanericacanada.daybook.model.Entry;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;

/**
 * Created by joanericacanada on 10/29/15.
 */
public class EntryListFragment extends ListFragment{
    //VARIABLES
    private ArrayList<Entry> mJournal;
    private ArrayList<Entry> mResult = new ArrayList<>();

    //WIDGETS
    private Spinner mSpinner;
    private EditText mEditSearch;
    private JournalAdapter mAdapter;

    @Override
    public void onResume(){
        super.onResume();
        Entry e = mJournal.get(mJournal.size()-1);
        if(e.getTitle() == null)
            mJournal.remove(e);
        chooseFilter(mSpinner.getSelectedItemPosition());
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        mJournal = EntryKeeper.get(getActivity()).getEntries();
        mResult.addAll(mJournal);

        //default sort: by date
        Collections.sort(mResult, new Comparator<Entry>() {
            @Override
            public int compare(Entry lhs, Entry rhs) {
                return rhs.getDate().compareTo(lhs.getDate());
            }
        });

        Toast.makeText(getContext(), "Welcome to DayBook!", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        View header = View.inflate(getContext(), R.layout.entry_list_fragment, null);
        mEditSearch = (EditText)header.findViewById(R.id.edtSearch);
        mEditSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                //do nothing
            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                search(s);
                mEditSearch.requestFocus();
            }
            @Override
            public void afterTextChanged(Editable s) {
                //do nothing
            }
        });
        mSpinner = (Spinner) header.findViewById(R.id.spinner);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), R.layout.spinner_layout,
                R.id.txtItem, getResources().getStringArray(R.array.filter));
        mSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                chooseFilter(position);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                //do nothing
            }
        });
        mSpinner.setAdapter(adapter);
        this.getListView().addHeaderView(header);
        mAdapter = new JournalAdapter(getContext(), mResult);
        setListAdapter(mAdapter);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        ((JournalAdapter)getListAdapter()).notifyDataSetChanged();
    }

    @Override
    public void onListItemClick(ListView l, View v,int pos, long id){
        Entry e = ((JournalAdapter)getListAdapter()).getItem(pos-1);

        Intent i = new Intent(getActivity(), EntryPagerActivity.class);
        i.putExtra(SelectedEntryFragment.ENTRY_ID, e.getId());
        startActivity(i);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater){
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_entry, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        Intent i;
        switch (item.getItemId()){
            case R.id.new_entry: // loads new entry
                Entry entry = new Entry();
                EntryKeeper.get(getActivity()).addEntry(entry);

                i = new Intent(getActivity(), EntryActivity.class);
                i.putExtra(EntryFragment.ENTRY_ID, entry.getId());
                startActivity(i);
                return true;
            case R.id.change_password: // change password
                i = new Intent(getActivity(), PasswordActivity.class);
                i.putExtra(PasswordActivity.CHANGE_PASSWORD, true);
                startActivity(i);
                return true;
            case R.id.sort:
                sortList();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    // sort list by date or title
    private void sortList(){
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.sort_text).setItems(R.array.sorter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, final int which) {
                Collections.sort(mResult, new Comparator<Entry>() {
                    @Override
                    public int compare(Entry lhs, Entry rhs) {
                        if(which == 0)
                            return rhs.getDate().compareTo(lhs.getDate());
                        else
                            return lhs.getTitle().compareTo(rhs.getTitle());
                    }
                });
                ((JournalAdapter)getListAdapter()).notifyDataSetChanged();
            }
        }).show();
    }

    // search list by keyword
    private void search(CharSequence cs){
        if (cs.toString().equals("")) chooseFilter(mSpinner.getSelectedItemPosition());
        else {
            mResult = new ArrayList<>();
            ArrayList<Entry> searchResult = new ArrayList<>();
            for(Entry e : mJournal) {
                if (e.getTitle().toLowerCase().contains(cs.toString().toLowerCase()))
                    searchResult.add(e);
            }
            mResult.addAll(searchResult);
        }
        setListAdapter(new JournalAdapter(getContext(), mResult));
        mAdapter.notifyDataSetChanged();
    }

    // filter listview by date: month, week, year
    private void chooseFilter(int choice){
        Calendar recent= Calendar.getInstance();
        Calendar entryDate= Calendar.getInstance();
        mResult = new ArrayList<>();

        if(choice == 0)
            mResult.addAll(mJournal);
        else{
            ArrayList<Entry> temp = new ArrayList<>();

            temp.addAll(mJournal);
            recent.setTime(new Date());

            for(int i = 1; i <= choice; i++){
                for(Entry e : temp) {
                    entryDate.setTime(e.getDate());
                    switch (i) {
                        case 1: // filter by year
                            if (entryDate.get(Calendar.YEAR) == recent.get(Calendar.YEAR))
                                mResult.add(e);
                            break;
                        case 2: // filter by month
                            if (entryDate.get(Calendar.MONTH) == recent.get(Calendar.MONTH))
                                mResult.add(e);
                            break;
                        case 3: // filter by week
                            if (entryDate.get(Calendar.WEEK_OF_MONTH) == recent.get(Calendar.WEEK_OF_MONTH))
                                mResult.add(e);
                            break;
                    }
                }
                if (i != choice){
                    temp = new ArrayList<>();
                    temp.addAll(mResult);
                    mResult = new ArrayList<>();
                }
            }
        }
        setListAdapter(new JournalAdapter(getContext(), mResult));
        ((JournalAdapter)getListAdapter()).notifyDataSetChanged();
    }
}
