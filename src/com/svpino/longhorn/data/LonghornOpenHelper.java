/*
 * Copyright (C) 2012 Santiago Valdarrama
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
 * 
 */
package com.svpino.longhorn.data;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.svpino.longhorn.R;

public class LonghornOpenHelper extends SQLiteOpenHelper {

	private static String DATABASE_DIRECTORY = "/data/data/com.svpino.longhorn/databases/";
	private static String DATABASE_NAME = "longhorn.rdb";
	private static String DATABASE_PATH = DATABASE_DIRECTORY + DATABASE_NAME;

	private final Context context;
	private boolean databaseNeedsToBeCreated;
	private boolean databaseNeedsToBeUpgraded;

	public LonghornOpenHelper(Context context) {
		super(context, DATABASE_NAME, null, context.getResources().getInteger(R.string.databaseVersion));
		this.context = context;
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		this.databaseNeedsToBeCreated = true;
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		this.databaseNeedsToBeUpgraded = true;
	}

	public void initialize() {
		getWritableDatabase();

		if (this.databaseNeedsToBeCreated) {
			try {
				copyDatabase();
			}
			catch (IOException e) {
			}
		}
		else if (this.databaseNeedsToBeUpgraded) {

		}
	}

	private void copyDatabase() throws IOException {
		close();

		InputStream inputStream = this.context.getAssets().open(DATABASE_NAME);
		OutputStream outputStream = new FileOutputStream(DATABASE_PATH);

		byte[] buffer = new byte[1024];
		int length;
		while ((length = inputStream.read(buffer)) > 0) {
			outputStream.write(buffer, 0, length);
		}

		outputStream.flush();
		outputStream.close();
		inputStream.close();

		getWritableDatabase().close();
	}

}
