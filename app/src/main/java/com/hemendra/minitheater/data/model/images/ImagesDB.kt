/*
 * Copyright (c) 2018 Hemendra Sharma
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hemendra.minitheater.data.model.images

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * We are going to convert them in blob (byte array) and save them into the database itself.
 * This way we can query and manage the images more easily and efficiently.
 */
class ImagesDB(ctx: Context) {

    companion object {

        /**
         * Maximum number of cover images we want to hold up in the cache.
         */
        private const val MAX_CACHED_IMAGES = 100

        private const val DATABASE_VERSION = 1
        private const val DATABASE_NAME = "ImagesDB"

        private const val TAB_IMAGES = "tab_images"

        private const val CREATE_TAB_IMAGES = ("create table if not exists  "
                + TAB_IMAGES
                + " ("
                + "_id integer primary key autoincrement,"  // primary key ID
                + "url text, "                              // the URL can be assumed unique
                + "data blob );")                           // byte array image data
    }

    private val dbHelper = DatabaseHelper(ctx)
    private var db: SQLiteDatabase = dbHelper.writableDatabase

    private class DatabaseHelper internal constructor(context: Context) :
            SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL("PRAGMA foreign_keys=ON")
            db.execSQL(CREATE_TAB_IMAGES)
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            upgradeDowngrade(db, oldVersion, newVersion)
        }

        override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int,
                                 newVersion: Int) {
            upgradeDowngrade(db, oldVersion, newVersion)
        }

        private fun upgradeDowngrade(db: SQLiteDatabase,
                                     oldVersion: Int,
                                     newVersion: Int) {
            db.execSQL("DROP TABLE IF EXISTS $TAB_IMAGES")
            onCreate(db)
        }
    }

    /**
     * Close the existing open connection.
     */
    fun close() {
        db.close()
    }

    /**
     * Insert the new image data byte array, if it does not exist already.
     * @param url the URL from where this image was downloaded.
     * @param data image byte array
     * @return the row ID of the newly inserted row, or -1 if an error occurred
     */
    fun insertImage(url: String, data: ByteArray): Long {
        if (url.isNotEmpty()) {
            var count = 0
            val countQuery = "SELECT count(*) FROM " + TAB_IMAGES + " WHERE url='" + url.trim { it <= ' ' } + "'"
            val c = db.rawQuery(countQuery, null)
            if (c != null) {
                if (c.moveToFirst() && c.columnCount > 0
                        && !c.isNull(0)) {
                    count = c.getInt(0)
                }
                c.close()
            }
            //
            if (count <= 0) {
                val values = ContentValues()
                if (data.isNotEmpty()) {
                    values.put("url", url)
                    values.put("data", data)
                    val ret = db.insert(TAB_IMAGES, null, values)
                    if (ret > 0) {
                        keepLastMaxImagesOnly()
                        return ret
                    }
                }
            }
        }
        return -1
    }

    /**
     * Keep the latest MAX_CACHED_IMAGES images and delete all other old images.
     */
    private fun keepLastMaxImagesOnly() {
        val countQuery = "SELECT count(*) FROM $TAB_IMAGES"
        var c: Cursor? = db.rawQuery(countQuery, null)
        if (c != null) {
            if (c.moveToFirst() && c.columnCount > 0 && !c.isNull(0)) {
                val totalImages = c.getInt(0)
                c.close()
                if (totalImages > MAX_CACHED_IMAGES) {
                    val query = "SELECT _id FROM $TAB_IMAGES ORDER BY _id ASC"
                    c = db.rawQuery(query, null)
                    if (c != null) {
                        if (c.moveToFirst() && c.columnCount > 0
                                && !c.isNull(0)) {
                            val diff = totalImages - MAX_CACHED_IMAGES
                            var count = 0
                            do {
                                db.delete(TAB_IMAGES, "_id=" + c.getInt(0), null)
                                count++
                            } while (c.moveToNext() && count < diff)
                        }
                        c.close()
                    }
                }
            }
        }
    }

    /**
     * Check if the image with the given URL exists in the DB or not,
     * and return the image byte array.
     * @param url the URL which is supposed to be used to download image from server.
     * @return the image byte array if exists. NULL otherwise.
     */
    fun getImage(url: String): ByteArray? {
        var bytes: ByteArray? = null
        if (url.isNotEmpty()) {
            val c = db.rawQuery("select * from " + TAB_IMAGES + " WHERE url='" + url.trim { it <= ' ' } + "'", null)
            if (c != null) {
                if (c.moveToFirst() && c.columnCount >= 3
                        && !c.isNull(2)) {
                    bytes = c.getBlob(2)
                }
                c.close()
            }
        }
        return bytes
    }

}
