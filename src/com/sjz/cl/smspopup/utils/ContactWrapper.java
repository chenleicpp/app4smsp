package com.sjz.cl.smspopup.utils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Contacts;
import android.provider.Contacts.PeopleColumns;
import android.provider.Contacts.Photos;
import android.util.Log;
import android.view.View;

public class ContactWrapper {

    private static final String TAG = "popup.ContactWrapper";

    // Reflection variables
    private static Class<?> contactsClass;
    private static Method contactsOpenPhotoStreamMethod;
    private static Method contactsGetLookupUriByContactUri;
    private static Method contactsGetLookupUri;

    private static Class<?> quickContactClass;
    private static Method showQuickContactMethod;
    public static int QUICKCONTACT_MODE_SMALL = 1;
    public static int QUICKCONTACT_MODE_MEDIUM = 2;
    public static int QUICKCONTACT_MODE_LARGE = 3;

    private static boolean preparedEclairSDK = false;

    // Projection columns
    private static final String CONTACT_ID = Contacts.People._ID;
    private static final String CONTACT_DISPLAY_NAME = PeopleColumns.DISPLAY_NAME;
    private static final String CONTACT_PERSON_ID = Contacts.Phones.PERSON_ID;
    private static final String CONTACT_ID_ECLAIR = "_id";
    private static final String CONTACT_DISPLAY_NAME_ECLAIR = "display_name";
    private static final String CONTACT_ID_EMAIL_ECLAIR = "contact_id";

    // Projection column ids
    public static final int COL_CONTACT_ID = 0;
    public static final int COL_DISPLAY_NAME = 1;
    public static final int COL_CONTACT_ID_EMAIL = 2;
    public static final int COL_CONTACT_PERSON_ID = 3;

    // Projections
    private static final String[] PEOPLE_PROJECTION = new String[] { CONTACT_ID, CONTACT_DISPLAY_NAME };

    private static final String[] PEOPLE_PROJECTION_ECLAIR = new String[] { CONTACT_ID_ECLAIR,
            CONTACT_DISPLAY_NAME_ECLAIR };

    private ContactWrapper() {
    }

    /**
     * Fetch regular contact content URI
     */
    public static Uri getContentUri() {
        if (PopupUtil.PRE_ECLAIR)
            return Contacts.People.CONTENT_URI;

        // ContactsContract.Contacts.CONTENT_URI
        return Uri.parse("content://com.android.contacts/contacts");
    }

    /**
     * Fetch phone lookup content filter URI
     */
    public static Uri getPhoneLookupContentFilterUri() {
        if (PopupUtil.PRE_ECLAIR) {
            return Contacts.Phones.CONTENT_FILTER_URL;
        }

        // ContactsContract.PhoneLookup.CONTENT_FILTER_URI
        return Uri.parse("content://com.android.contacts/phone_lookup");
    }

    public static String[] getPhoneLookupProjection() {
        if (PopupUtil.PRE_ECLAIR) {
            return new String[] { Contacts.Phones.PERSON_ID, Contacts.Phones.DISPLAY_NAME };
        }

        /*
         * ContactsContract.PhoneLookup._ID
         * ContactsContract.PhoneLookup.DISPLAY_NAME
         * ContactsContract.PhoneLookup.LOOKUP_KEY
         */
        return new String[] { "_id", "display_name", "lookup" };
    }

    /**
     * Fetch email lookup content filter URI
     */
    public static Uri getEmailLookupContentFilterUri() {
        if (PopupUtil.PRE_ECLAIR) {
            return Uri.parse("content://contacts/people/with_email_or_im_filter");
        }

        // ContactsContract.CommonDataKinds.Email.CONTENT_LOOKUP_URI
        return Uri.parse("content://com.android.contacts/data/emails/lookup");
    }

    public static String[] getEmailLookupProjection() {
        if (PopupUtil.PRE_ECLAIR) {
            return new String[] { Contacts.People._ID, Contacts.People.DISPLAY_NAME };
        }

        /*
         * ContactsContract.CommonDataKinds.Email.CONTACT_ID
         * ContactsContract.CommonDataKinds.Email.DISPLAY_NAME
         * ContactsContract.CommonDataKinds.Email.LOOKUP_KEY
         */
        return new String[] { "contact_id", "display_name", "lookup" };
    }

    /**
     * Fetch default sort order for contacts
     */
    public static String getDefaultSortOrder() {
        if (PopupUtil.PRE_ECLAIR) {
            return Contacts.People.DEFAULT_SORT_ORDER;
        }

        return null;
    }

    /**
     * Fetch base contact projection (id and display name)
     */
    public static String[] getBasePeopleProjection() {
        if (PopupUtil.PRE_ECLAIR) {
            return PEOPLE_PROJECTION;
        }

        return PEOPLE_PROJECTION_ECLAIR;
    }

    /**
     * Fetch contact table column
     */
    public static String getColumn(int col) {
        if (PopupUtil.PRE_ECLAIR) {
            switch (col) {
            case COL_CONTACT_ID:
                return CONTACT_ID;

            case COL_DISPLAY_NAME:
                return CONTACT_DISPLAY_NAME;

            case COL_CONTACT_ID_EMAIL:
                return CONTACT_ID;

            case COL_CONTACT_PERSON_ID:
                return CONTACT_PERSON_ID;
            }

            return null;
        }

        switch (col) {
        case COL_CONTACT_ID:
            return CONTACT_ID_ECLAIR;

        case COL_DISPLAY_NAME:
            return CONTACT_DISPLAY_NAME_ECLAIR;

        case COL_CONTACT_ID_EMAIL:
            return CONTACT_ID_EMAIL_ECLAIR;

        case COL_CONTACT_PERSON_ID:
            return CONTACT_ID_ECLAIR;
        }

        return null;
    }

    /**
     * Returns an InputStream for the person's photo
     * 
     * @param id
     *            the id of the person
     */
    public static InputStream openContactPhotoInputStream(ContentResolver cr, String id) {
        if (id == null)
            return null;
        if ("0".equals(id))
            return null;

        Cursor cursor;
        Uri photoUri;

        // If we're pre-Eclair then lookup the contact using the standard URIs
        if (PopupUtil.PRE_ECLAIR) {

            /*
             * Contacts.People.CONTENT_URI is "content://contacts/people"
             * Contacts.Photos.CONTENT_DIRECTORY is "photo"; Uri will end up
             * being "content://contacts/people/#contactId/photo"
             */
            Log.d(TAG, "openContactPhotoInputStream(): looking in Contacts.People");
            photoUri = Uri.withAppendedPath(Uri.withAppendedPath(Contacts.People.CONTENT_URI, id),
                    Contacts.Photos.CONTENT_DIRECTORY);

            cursor = cr.query(photoUri, new String[] { Photos.DATA }, null, null, null);

            if (cursor != null) {
                try {
                    if (cursor.moveToFirst()) {
                        byte[] data = cursor.getBlob(0);
                        if (data != null) {
                            Log.d(TAG, "openContactPhotoInputStream(): contact photo found");
                            return new ByteArrayInputStream(data);
                        }
                    }
                } finally {
                    cursor.close();
                }
            }

            Log.d(TAG, "openContactPhotoInputStream(): looking in Contacts.Photos");
            /*
             * Contacts.Photos.CONTENT_URI is "content://contacts/photos" Uri
             * will end up being "content://contacts/photos/#contactId"
             */
            photoUri = Uri.withAppendedPath(Contacts.Photos.CONTENT_URI, id);

            cursor = cr.query(photoUri, new String[] { Photos.DATA }, null, null, null);

            if (cursor != null) {
                try {
                    if (cursor.moveToFirst()) {
                        byte[] data = cursor.getBlob(0);
                        if (data != null) {
                            Log.d(TAG,"openContactPhotoInputStream(): contact photo found");
                            return new ByteArrayInputStream(data);
                        }
                    }
                } finally {
                    cursor.close();
                }
            }

            return null;
        }

        /*
         * User is using Eclair or beyond, use reflection to pull the photo
         */
        if (!preparedEclairSDK) {
            prepareEclairSDK();
        }

        if (preparedEclairSDK) {
            try {
                InputStream photoStream = (InputStream) contactsOpenPhotoStreamMethod.invoke(contactsClass, cr,
                        Uri.withAppendedPath(getContentUri(), id));

                return photoStream;
                /*
                 * if (photoStream != null) { if (Log.DEBUG) Log.v(
                 * "openContactPhotoInputStream(): contact photo found using Eclair SDK"
                 * ); return photoStream; }
                 */
            } catch (IllegalArgumentException e) {
                Log.d(TAG,"Unable to fetch contact photo using Anroid 2.0+ SDK: " + e.toString());
            } catch (IllegalAccessException e) {
                Log.d(TAG,"Unable to fetch contact photo using Anroid 2.0+ SDK: " + e.toString());
            } catch (InvocationTargetException e) {
                Log.d(TAG,"Unable to fetch contact photo using Anroid 2.0+ SDK: " + e.toString());
            }

        }

        return null;
    }

    public static Uri getLookupUri(long contactId, String lookupKey) {

        if (PopupUtil.PRE_ECLAIR)
            return null;

        if (!preparedEclairSDK) {
            prepareEclairSDK();
        }

        Uri lookupUri = null;
        try {
            lookupUri = (Uri) contactsGetLookupUri.invoke(contactsClass, contactId, lookupKey);
        } catch (IllegalArgumentException e) {
            Log.d(TAG,"Unable to get contact lookup Uri using Anroid 2.0+ SDK: " + e.toString());
        } catch (IllegalAccessException e) {
            Log.d(TAG,"Unable to get contact lookup Uri using Anroid 2.0+ SDK: " + e.toString());
        } catch (InvocationTargetException e) {
            Log.d(TAG,"Unable to get contact lookup Uri using Anroid 2.0+ SDK: " + e.toString());
        }

        return lookupUri;
    }

    public static Uri getLookupUri(ContentResolver cr, Uri contactUri) {

        if (PopupUtil.PRE_ECLAIR)
            return null;

        if (!preparedEclairSDK) {
            prepareEclairSDK();
        }

        Uri lookupUri = null;
        try {
            lookupUri = (Uri) contactsGetLookupUriByContactUri.invoke(contactsClass, cr, contactUri);
        } catch (IllegalArgumentException e) {
            Log.d(TAG,"Unable to get contact lookup Uri using Anroid 2.0+ SDK: " + e.toString());
        } catch (IllegalAccessException e) {
            Log.d(TAG,"Unable to get contact lookup Uri using Anroid 2.0+ SDK: " + e.toString());
        } catch (InvocationTargetException e) {
            Log.d(TAG,"Unable to get contact lookup Uri using Anroid 2.0+ SDK: " + e.toString());
        }

        return lookupUri;
    }

    public static void showQuickContact(Context context, View target, Uri lookupUri, int mode, String[] excludeMimes) {

        if (PopupUtil.PRE_ECLAIR)
            return;

        if (!preparedEclairSDK) {
            prepareEclairSDK();
        }

        try {
            showQuickContactMethod.invoke(quickContactClass, context, target, lookupUri, mode, excludeMimes);
        } catch (IllegalArgumentException e) {
            Log.d(TAG,"Unable to get show quick contact dialog using Anroid 2.0+ SDK: " + e.toString());
        } catch (IllegalAccessException e) {
            Log.d(TAG,"Unable to get show quick contact dialog using Anroid 2.0+ SDK: " + e.toString());
        } catch (InvocationTargetException e) {
            Log.d(TAG,"Unable to get show quick contact dialog using Anroid 2.0+ SDK: " + e.toString());
        }

    }

    /*
     * Prepare Eclair (and beyond) SDK call using reflection
     */
    private static void prepareEclairSDK() {
        try {

            // ContactsContract.Contacts class and methods
            contactsClass = Class.forName("android.provider.ContactsContract$Contacts");

            contactsOpenPhotoStreamMethod = contactsClass.getMethod("openContactPhotoInputStream", new Class[] {
                    ContentResolver.class, Uri.class });

            contactsGetLookupUri = contactsClass.getMethod("getLookupUri", new Class[] { long.class, String.class });

            contactsGetLookupUriByContactUri = contactsClass.getMethod("getLookupUri", new Class[] {
                    ContentResolver.class, Uri.class });

            // ContactsContract.QuickContacts class and method
            quickContactClass = Class.forName("android.provider.ContactsContract$QuickContact");

            showQuickContactMethod = quickContactClass.getMethod("showQuickContact", new Class[] { Context.class,
                    View.class, Uri.class, int.class, String[].class });

            preparedEclairSDK = true;

        } catch (ClassNotFoundException e) {
            Log.d(TAG,"Unable to prepare Anroid 2.0+ SDK functions: " + e.toString());
        } catch (SecurityException e) {
            Log.d(TAG,"Unable to prepare Anroid 2.0+ SDK functions: " + e.toString());
        } catch (NoSuchMethodException e) {
            Log.d(TAG,"Unable to prepare Anroid 2.0+ SDK functions: " + e.toString());
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }
}