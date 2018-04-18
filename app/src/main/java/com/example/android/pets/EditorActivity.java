/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.android.pets;

import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.example.android.pets.data.PetContract.PetEntry;

/**
 * Allows user to create a new pet or edit an existing one.
 */
public class EditorActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {
    //Tag for log statements
    public static final String LOG_TAG = EditorActivity.class.getName();

    //Loader ID
    public static final int PET_LOADER = 0;

    Uri mCurrentPetUri;

    /**
     * EditText field to enter the pet's name
     */
    private EditText mNameEditText;

    /**
     * EditText field to enter the pet's breed
     */
    private EditText mBreedEditText;

    /**
     * EditText field to enter the pet's weight
     */
    private EditText mWeightEditText;

    /**
     * EditText field to enter the pet's gender
     */
    private Spinner mGenderSpinner;

    /**
     * Gender of the pet. The possible values are:
     * 0 for unknown gender, 1 for male, 2 for female.
     */
    private int mGender = 0;

    /**
     * Boolean to check whether the user is creating a pet or not
     */
    private boolean mPetHasChanged = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        //Use getIntent() and getData() to get the associated URI
        Intent intent = getIntent();

        mCurrentPetUri = intent.getData();
        //
        if (mCurrentPetUri == null) {
            setTitle(R.string.editor_activity_title_new_pet);
            //Remove the delete pet option
            invalidateOptionsMenu();
        } else {
            setTitle(R.string.editor_activity_title_edit_pet);
            //Initialize the loader if uri is not null
            getLoaderManager().initLoader(PET_LOADER, null, this);
        }

        Log.i(LOG_TAG, "Intent\'s pet URI: " + mCurrentPetUri);

        // Find all relevant views that we will need to read user input from
        mNameEditText = (EditText) findViewById(R.id.edit_pet_name);
        mBreedEditText = (EditText) findViewById(R.id.edit_pet_breed);
        mWeightEditText = (EditText) findViewById(R.id.edit_pet_weight);
        mGenderSpinner = (Spinner) findViewById(R.id.spinner_gender);

        //Set up onTouchListeners
        mWeightEditText.setOnTouchListener(mTouchListener);
        mBreedEditText.setOnTouchListener(mTouchListener);
        mWeightEditText.setOnTouchListener(mTouchListener);
        mGenderSpinner.setOnTouchListener(mTouchListener);

        //Set up the spinner
        setupSpinner();

    }

    private View.OnTouchListener mTouchListener = new View.OnTouchListener() {
        // OnTouchListener that listens for any user touches on a View, implying that they are modifying
        // the view, and we change the mPetHasChanged boolean to true.
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            mPetHasChanged = true;
            return false;
        }
    };

    /**
     * Setup the dropdown spinner that allows the user to select the gender of the pet.
     */
    private void setupSpinner() {
        // Create adapter for spinner. The list options are from the String array it will use
        // the spinner will use the default layout
        ArrayAdapter genderSpinnerAdapter = ArrayAdapter.createFromResource(this,
                R.array.array_gender_options, android.R.layout.simple_spinner_item);

        // Specify dropdown layout style - simple list view with 1 item per line
        genderSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line);

        // Apply the adapter to the spinner
        mGenderSpinner.setAdapter(genderSpinnerAdapter);

        // Set the integer mSelected to the constant values
        mGenderSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selection = (String) parent.getItemAtPosition(position);
                if (!TextUtils.isEmpty(selection)) {
                    if (selection.equals(getString(R.string.gender_male))) {
                        mGender = PetEntry.GENDER_MALE; // Male
                    } else if (selection.equals(getString(R.string.gender_female))) {
                        mGender = PetEntry.GENDER_FEMALE; // Female
                    } else {
                        mGender = PetEntry.GENDER_UNKNOWN; // Unknown
                    }
                }
            }

            // Because AdapterView is an abstract class, onNothingSelected must be defined
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                mGender = 0; // Unknown
            }
        });
    }

    /**
     * Save pet method for when the Save button is pressed
     */
    private void savePet() {
        // Read from input fields
        // Use trim to eliminate leading or trailing white space
        String name = mNameEditText.getText().toString().trim();
        String breed = mBreedEditText.getText().toString().trim();
        //Gender is already defined in the mGender variable that takes the input from the spinner
        String weightString = mWeightEditText.getText().toString().trim();
        int weight = 0;
        //If the weight string is empty set the weight to 0, this avoids unwanted bugs
        if (!TextUtils.isEmpty(weightString)) {
            weight = Integer.parseInt(weightString);
        }

        //If no information was inserted, finish the activity and return to the CatalogActivity
        if (TextUtils.isEmpty(name) && TextUtils.isEmpty(breed) &&
                weight == 0 && mGender == PetEntry.GENDER_UNKNOWN) {
            return;
        }

        //Create the Content Values Object
        ContentValues values = new ContentValues();
        //Insert data into the values Object
        values.put(PetEntry.COLUMN_PET_NAME, name);
        values.put(PetEntry.COLUMN_PET_BREED, breed);
        values.put(PetEntry.COLUMN_PET_GENDER, mGender);
        values.put(PetEntry.COLUMN_PET_WEIGHT, weight);

        //Insert or saved a new pet whether the user is creating a new pet or
        // editing an existing one
        if (mCurrentPetUri == null) {
            // Insert a new pet in the database, returning a new Uri for the row
            Uri newUri = getContentResolver().insert(PetEntry.CONTENT_URI, values);

            //Toast message indicating the result
            if (newUri == null) {
                Toast.makeText(this, R.string.pet_not_saved, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, R.string.pet_saved, Toast.LENGTH_SHORT).show();
            }
        } else {
            int petsChanged = getContentResolver().update(
                    mCurrentPetUri,
                    values,
                    null,
                    null);
            //Toast message indicating the result
            if (petsChanged == 0) {
                Toast.makeText(this, R.string.pet_not_saved, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, R.string.pet_saved, Toast.LENGTH_SHORT).show();
            }
        }

    }

    /**
     * Perform the deletion of the pet in the database.
     */
    private void deletePet() {
        //Only delete if it's an existing pet.
        if (mCurrentPetUri != null) {

            int rowsDeleted = getContentResolver().delete(
                    mCurrentPetUri,
                    null,
                    null
            );

            if (rowsDeleted == 0) {
                Toast.makeText(this, R.string.editor_delete_pet_failed, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, R.string.editor_delete_pet_successful, Toast.LENGTH_SHORT).show();
            }
        }

        //Close the activity.
        finish();

    }

    /***********************************
     **** Confirmation Dialog Methods **
     ***********************************/

    private void showUnsavedChangesDialog(
            DialogInterface.OnClickListener discardButtonClickListener) {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the positive and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.unsaved_changes_dialog_msg);
        builder.setPositiveButton(R.string.discard, discardButtonClickListener);
        builder.setNegativeButton(R.string.keep_editing, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                //User clicked "Keep Editing" so dismiss the dialog
                //and continue editing the pet.
                if (dialogInterface != null) {
                    dialogInterface.dismiss();
                }
            }
        });

        //Create and show the Alert Dialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private void showDeleteConfirmationDialog() {
        // Create an AlertDialog.Builder and set the message and click listeners
        // for the positive and negative buttons on the dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.delete_dialog_msg);
        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                // User selected the "Delete" button so delete the pet
                deletePet();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                // User clicked the "Cancel" button, so dismiss the dialog
                // and continue editing the pet.
                if (dialogInterface != null) {
                    dialogInterface.dismiss();
                }
            }
        });

        //Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    /**************************
     **** Overridden Methods **
     **************************/

    @Override
    public void onBackPressed() {
        // If the pet hasn't changed, continue with handling back button press
        if (!mPetHasChanged) {
            super.onBackPressed();
            return;
        }
        // Otherwise if there are unsaved changes, setup a dialog to warn the user.
        // Create a click listener to handle the user confirming that changes should be discarded.
        DialogInterface.OnClickListener discardButtonClickListener =
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        finish();
                    }
                };
        // Show dialog that there are unsaved changes
        showUnsavedChangesDialog(discardButtonClickListener);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu options from the res/menu/menu_editor.xml file.
        // This adds menu items to the app bar.
        getMenuInflater().inflate(R.menu.menu_editor, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        //If this is a new pet hide the "Delete" menu item.
        if (mCurrentPetUri == null) {
            MenuItem menuItem = menu.findItem(R.id.action_delete);
            menuItem.setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // User clicked on a menu option in the app bar overflow menu
        switch (item.getItemId()) {
            // Respond to a click on the "Save" menu option
            case R.id.action_save:
                //Save pet to database
                savePet();
                //Return to catalog activity
                finish();
                return true;
            // Respond to a click on the "Delete" menu option
            case R.id.action_delete:
                // Do nothing for now
                showDeleteConfirmationDialog();
                return true;
            // Respond to a click on the "Up" arrow button in the app bar
            case android.R.id.home:
                // If the pet hasn't changed, continue with navigating up to parent activity
                // which is the {@link CatalogActivity}.
                if (!mPetHasChanged) {
                    NavUtils.navigateUpFromSameTask(this);
                    return true;
                }
                // Otherwise if there are unsaved changes, setup a dialog to warn the user.
                // Create a click listener to handle the user confirming that
                // changes should be discarded.
                DialogInterface.OnClickListener discardButtonClickListener =
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                NavUtils.navigateUpFromSameTask(EditorActivity.this);
                            }
                        };
                // Show a dialog that notifies the user they have unsaved changes
                showUnsavedChangesDialog(discardButtonClickListener);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int loaderID, Bundle bundle) {

        switch (loaderID) {
            case PET_LOADER:
                String[] projection = {
                        PetEntry.COLUMN_PET_NAME,
                        PetEntry.COLUMN_PET_BREED,
                        PetEntry.COLUMN_PET_GENDER,
                        PetEntry.COLUMN_PET_WEIGHT
                };
                Log.i(LOG_TAG, "THE URI TO BE USED: " + mCurrentPetUri);
                return new CursorLoader(
                        this,
                        mCurrentPetUri,
                        projection,
                        null,
                        null,
                        null
                );
            //Wrong ID has been passed
            default:
                return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        //Prevent crash when there are now rows after deleting.
        if (cursor == null || cursor.getCount() < 1 ){
            return;
        }
        //Move the cursor to the next position
        cursor.moveToNext();
        //Get the index of the cursor's columns
        int nameIndex = cursor.getColumnIndex(PetEntry.COLUMN_PET_NAME);
        int breedIndex = cursor.getColumnIndex(PetEntry.COLUMN_PET_BREED);
        int genderIndex = cursor.getColumnIndex(PetEntry.COLUMN_PET_GENDER);
        int weightIndex = cursor.getColumnIndex(PetEntry.COLUMN_PET_WEIGHT);
        //Get the values of the columns
        String currentPetName = cursor.getString(nameIndex);
        String currentPetBreed = cursor.getString(breedIndex);
        int currentPetGender = cursor.getInt(genderIndex);
        int currentPetWeight = cursor.getInt(weightIndex);
        //Assign the values to the corresponding fields
        mNameEditText.setText(currentPetName);
        mBreedEditText.setText(currentPetBreed);
        mGenderSpinner.setSelection(currentPetGender);
        mWeightEditText.setText(String.format("%d", currentPetWeight));
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        //Clear the input fields
        mNameEditText.getText().clear();
        mBreedEditText.getText().clear();
        mGenderSpinner.setSelection(0);
        mWeightEditText.getText().clear();
    }
}