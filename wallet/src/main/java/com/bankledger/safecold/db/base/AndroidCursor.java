/*
 * Copyright 2014 http://Bither.net
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bankledger.safecold.db.base;

import android.database.Cursor;

import com.bankledger.safecoldj.db.impl.base.ICursor;


public class AndroidCursor implements ICursor {

    private Cursor cursor;

    public AndroidCursor(Cursor cursor) {
        this.cursor = cursor;
    }

    @Override
    public int getCount() {
        return cursor.getCount();
    }

    @Override
    public boolean moveToNext() {
        return cursor.moveToNext();
    }

    @Override
    public int getColumnIndex(String var1) {
        return cursor.getColumnIndex(var1);
    }

    @Override
    public int getColumnIndexOrThrow(String var1) throws IllegalArgumentException {
        return cursor.getColumnIndexOrThrow(var1);
    }

    @Override
    public byte[] getBlob(int var1) {
        return cursor.getBlob(var1);
    }

    @Override
    public String getString(int var1) {
        return cursor.getString(var1);
    }

    @Override
    public short getShort(int var1) {
        return cursor.getShort(var1);
    }

    @Override
    public int getInt(int var1) {
        return cursor.getInt(var1);
    }

    @Override
    public long getLong(int var1) {
        return cursor.getLong(var1);
    }

    @Override
    public float getFloat(int var1) {
        return cursor.getFloat(var1);
    }

    @Override
    public double getDouble(int var1) {
        return cursor.getDouble(var1);
    }

    @Override
    public int getType(int var1) {
        return cursor.getType(var1);
    }

    @Override
    public boolean isNull(int var1) {
        return cursor.isNull(var1);
    }

    @Override
    public void close() {
        cursor.close();
    }

    @Override
    public boolean isClosed() {
        return cursor.isClosed();
    }
}
