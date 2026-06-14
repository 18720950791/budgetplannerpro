package com.twansoftware.budgetplannerpro.iface;

import android.content.ContentValues;
import android.database.Cursor;

public interface CursorConvertable<E> {
    E cursorToObject(final Cursor cursor);
    ContentValues objectToContentValues(final E object);
}
