/*
 * Copyright (c) 2014, Apptentive, Inc. All Rights Reserved.
 * Please refer to the LICENSE file for the terms and conditions
 * under which redistribution and use of this file is permitted.
 */

package com.apptentive.android.sdk.model;

import android.content.Context;
import android.content.SharedPreferences;

import com.apptentive.android.sdk.ApptentiveInternal;
import com.apptentive.android.sdk.ApptentiveLog;
import com.apptentive.android.sdk.util.Constants;
import com.apptentive.android.sdk.util.Util;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * <p>All public methods altering code point values should be synchronized.</p>
 * <p>Example:</p>
 * <pre>
 * {
 *   "code_point": {
 *     "codePoint1": {
 *       "last": 1234567890,
 *       "total": 6,
 *       "version": {
 *         "1.1": 4,
 *         "1.2": 2
 *       },
 *       "build": {
 *         "5": 4,
 *         "6": 2
 *       }
 *     }
 *   },
 *   "interactions": {
 *     "526fe2836dd8bf546a00000c": {
 *       "last": 1234567890.4,
 *       "total": 6,
 *       "version": {
 *         "1.1": 4,
 *         "1.2": 2
 *       },
 *       "build": {
 *         "5": 4,
 *         "6": 2
 *       }
 *     }
 *   }
 * }
 * </pre>
 *
 * @author Sky Kelsey
 */
public class CodePointStore {

	private JSONObject store;

	public static final String KEY_CODE_POINT = "code_point";
	public static final String KEY_INTERACTIONS = "interactions";
	public static final String KEY_LAST = "last"; // The last time this codepoint was seen.
	public static final String KEY_TOTAL = "total"; // The total times this code point was seen.
	public static final String KEY_VERSION = "version";
	public static final String KEY_BUILD = "build";

	public CodePointStore() {

	}

	public void init() {
		store = loadFromPreference();
	}

	private void saveToPreference() {
		SharedPreferences prefs = ApptentiveInternal.getInstance().getSharedPrefs();
		prefs.edit().putString(Constants.PREF_KEY_CODE_POINT_STORE, store.toString()).apply();
	}

	private JSONObject loadFromPreference() {
		SharedPreferences prefs = ApptentiveInternal.getInstance().getSharedPrefs();
		String json = prefs.getString(Constants.PREF_KEY_CODE_POINT_STORE, null);
		try {
			if (json != null) {
				return new JSONObject(json);
			}
		} catch (JSONException e) {
			ApptentiveLog.e("Error loading CodePointStore from SharedPreferences.", e);
		}
		return new JSONObject();
	}


	public synchronized void storeCodePointForCurrentAppVersion(String fullCodePoint) {
		storeRecordForCurrentAppVersion(false, fullCodePoint);
	}

	public synchronized void storeInteractionForCurrentAppVersion(String fullCodePoint) {
		storeRecordForCurrentAppVersion(true, fullCodePoint);
	}

	private void storeRecordForCurrentAppVersion(boolean isInteraction, String fullCodePoint) {
		Context context = ApptentiveInternal.getInstance().getApplicationContext();
		String version = Util.getAppVersionName(context);
		int build = Util.getAppVersionCode(context);
		storeRecord(isInteraction, fullCodePoint, version, build);
	}

	public synchronized void storeRecord(boolean isInteraction, String fullCodePoint, String version, int build) {
		storeRecord(isInteraction, fullCodePoint, version, build, Util.currentTimeSeconds());
	}

	public synchronized void storeRecord(boolean isInteraction, String fullCodePoint, String version, int build, double currentTimeSeconds) {
		String buildString = String.valueOf(build);
		if (fullCodePoint != null && version != null) {
			try {
				String recordTypeKey = isInteraction ? CodePointStore.KEY_INTERACTIONS : CodePointStore.KEY_CODE_POINT;
				JSONObject recordType;
				if (!store.isNull(recordTypeKey)) {
					recordType = store.getJSONObject(recordTypeKey);
				} else {
					recordType = new JSONObject();
					store.put(recordTypeKey, recordType);
				}

				// Get or create code point object.
				JSONObject codePointJson;
				if (!recordType.isNull(fullCodePoint)) {
					codePointJson = recordType.getJSONObject(fullCodePoint);
				} else {
					codePointJson = new JSONObject();
					recordType.put(fullCodePoint, codePointJson);
				}

				// Set the last time this code point was seen.
				codePointJson.put(KEY_LAST, currentTimeSeconds);

				// Increment the total times this code point was seen.
				int total = 0;
				if (codePointJson.has(KEY_TOTAL)) {
					total = codePointJson.getInt(KEY_TOTAL);
				}
				codePointJson.put(KEY_TOTAL, total + 1);

				// Get or create version object.
				JSONObject versionJson;
				if (!codePointJson.isNull(KEY_VERSION)) {
					versionJson = codePointJson.getJSONObject(KEY_VERSION);
				} else {
					versionJson = new JSONObject();
					codePointJson.put(KEY_VERSION, versionJson);
				}

				// Set count for current version.
				int existingVersionCount = 0;
				if (!versionJson.isNull(version)) {
					existingVersionCount = versionJson.getInt(version);
				}
				versionJson.put(version, existingVersionCount + 1);

				// Get or create build object.
				JSONObject buildJson;
				if (!codePointJson.isNull(KEY_BUILD)) {
					buildJson = codePointJson.getJSONObject(KEY_BUILD);
				} else {
					buildJson = new JSONObject();
					codePointJson.put(KEY_BUILD, buildJson);
				}

				// Set count for the current build
				int existingBuildCount = 0;
				if (!buildJson.isNull(buildString)) {
					existingBuildCount = buildJson.getInt(buildString);
				}
				buildJson.put(buildString, existingBuildCount + 1);

				saveToPreference();
			} catch (JSONException e) {
				ApptentiveLog.w("Unable to store code point %s.", e, fullCodePoint);
			}
		}
	}

	public JSONObject getRecord(boolean interaction, String name) {
		String recordTypeKey = interaction ? KEY_INTERACTIONS : KEY_CODE_POINT;
		try {
			if (!store.isNull(recordTypeKey)) {
				if (store.has(recordTypeKey)) {
					JSONObject recordType = store.getJSONObject(recordTypeKey);
					if (recordType.has(name)) {
						return recordType.getJSONObject(name);
					}
				}
			}
		} catch (JSONException e) {
			ApptentiveLog.w("Error loading code point record for \"%s\"", name);
		}
		return null;
	}

	public Long getTotalInvokes(boolean interaction, String name) {
		try {
			JSONObject record = getRecord(interaction, name);
			if (record != null && record.has(KEY_TOTAL)) {
				return record.getLong(KEY_TOTAL);
			}
		} catch (JSONException e) {
			// Ignore
		}
		return 0l;
	}

	public Double getLastInvoke(boolean interaction, String name) {
		try {
			JSONObject record = getRecord(interaction, name);
			if (record != null && record.has(KEY_LAST)) {
				return record.getDouble(KEY_LAST);
			}
		} catch (JSONException e) {
			// Ignore
		}
		return null;
	}

	public Long getVersionInvokes(boolean interaction, String name, String version) {
		try {
			JSONObject record = getRecord(interaction, name);
			if (record != null && record.has(KEY_VERSION)) {
				JSONObject versionJson = record.getJSONObject(KEY_VERSION);
				if (versionJson.has(version)) {
					return versionJson.getLong(version);
				}
			}
		} catch (JSONException e) {
			// Ignore
		}
		return 0l;
	}

	public Long getBuildInvokes(boolean interaction, String name, String build) {
		try {
			JSONObject record = getRecord(interaction, name);
			if (record != null && record.has(KEY_BUILD)) {
				JSONObject buildJson = record.getJSONObject(KEY_BUILD);
				if (buildJson.has(build)) {
					return buildJson.getLong(build);
				}
			}
		} catch (JSONException e) {
			// Ignore
		}
		return 0L;
	}

	public String toString() {
		return "CodePointStore:  " + store.toString();
	}

	public void clear() {
		SharedPreferences prefs = ApptentiveInternal.getInstance().getSharedPrefs();
		prefs.edit().remove(Constants.PREF_KEY_CODE_POINT_STORE).apply();
		store = new JSONObject();
	}

}