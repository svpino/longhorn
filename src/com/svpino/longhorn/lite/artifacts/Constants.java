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
package com.svpino.longhorn.lite.artifacts;

public class Constants {
	public final static String PREFERENCES = "longhorn_preferences";

	public final static String PREFERENCE_TERMS_AND_CONDITIONS = "accepted_terms_and_conditions";

	public final static String PREFERENCE_COLLECTOR_LAST_UPDATE = "collector_last_update";
	public final static String PREFERENCE_COLLECTOR_RETRYING = "collector_retrying";
	public final static String PREFERENCE_COLLECTOR_RETRIES = "collector_retries";

	public final static String PREFERENCE_STATUS_WAITING_FOR_CONNECTIVITY = "status_waiting_for_connectivity";

	public final static long COLLECTOR_MIN_REFRESH_INTERVAL = 15 * 60 * 1000;
	public final static long COLLECTOR_MAX_REFRESH_INTERVAL = 24 * 60 * 60 * 1000;
	public final static long COLLECTOR_MIN_RETRY_INTERVAL = (long) (0.5 * 60 * 1000);

	public final static String SCHEDULE_RETRY = "schedule_retry";
	public final static String SCHEDULE_AUTOMATIC = "schedule_automatic";
	public final static String SCHEDULE_BACKGROUND = "schedule_background";

}
