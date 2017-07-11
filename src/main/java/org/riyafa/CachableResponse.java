/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.riyafa;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This object holds the cached response and the related properties of the cache
 * per request and will be stored in to the cache. This holds the response envelope
 * together with the request hash and the response hash. Apart from that this object
 * holds the refresh time of the cache and the timeout period. This implements the
 * Serializable interface to support the clustered caching.
 *
 * @see Serializable
 */
public class CachableResponse implements Serializable {

	private String responsePayload;

	/**
	 * This boolean value defines whether this cached object is in use or not
	 * Cache cleanup method will not remove cached object if cached object is in use
	 * Upon cache hit, cached object will set inUse as true and set inUse as false after response
	 * is served by cached object
	 */
	private AtomicBoolean inUse = new AtomicBoolean(false);

	/**
	 * This holds the hash value of the request payload which is calculated form
	 * the specified DigestGenerator, and is used to index the cached response
	 */
	private String requestHash;

	/**
	 * This holds the time at which this particular cached response expires, in
	 * the standard java system time format (i.e. System.currentTimeMillis())
	 */
	private long expireTimeMillis;

	/**
	 * This holds the timeout period of the cached response which will be used
	 * at the next refresh time in order to generate the expireTimeMillis
	 */
	private long timeout;

	/**
	 * This holds the HTTP Header Properties of the response.
	 * */
	private Map<String,Object> headerProperties;

	/**
	 * This method checks whether this cached response is expired or not
	 *
	 * @return boolean true if expired and false if not
	 */
	public boolean isExpired() {
		return timeout <= 0 || expireTimeMillis < System.currentTimeMillis();
	}

	/**
	 * This method will refresh the cached response stored in this object.
	 * If further explained this method will set the response envelope and the
	 * response hash to null and set the new refresh time as timeout + current time
	 *
	 * This is how an expired response is brought back to life
	 * @param timeout The period for which this object is reincarnated
	 */
	public void reincarnate(long timeout) {
		if(!isExpired()){
			throw new IllegalStateException("Unexpired Cached Responses cannot be reincarnated");
		}
		responsePayload = null;
		headerProperties = null;
		expireTimeMillis = System.currentTimeMillis() + timeout;
		setTimeout(timeout);
	}

	public String getResponsePayload() {
		return responsePayload;
	}

	public void setResponsePayload(String responsePayload) {
		this.responsePayload = responsePayload;
	}

	/**
	 * This method gives the hash value of the request payload stored in the cache
	 *
	 * @return String hash of the request payload
	 */
	public String getRequestHash() {
		return requestHash;
	}

	/**
	 * This method sets the hash of the request to the cache
	 *
	 * @param requestHash   - hash of the request payload to be set as an String
	 */
	public void setRequestHash(String requestHash) {
		this.requestHash = requestHash;
	}

	/**
	 * This method gives the expireTimeMillis in the standard java system time format
	 *
	 * @return long refresh time in the standard java system time format
	 */
	public long getExpireTimeMillis() {
		return expireTimeMillis;
	}

	/**
	 * This method sets the refresh time to the cached response
	 *
	 * @param expireTimeMillis    - refresh time in the standard java system time format
	 */
	public void setExpireTimeMillis(long expireTimeMillis) {
		this.expireTimeMillis = expireTimeMillis;
	}

	/**
	 * This method gives the timeout period in milliseconds
	 *
	 * @return timeout in milliseconds
	 */
	public long getTimeout() {
		return timeout;
	}

	/**
	 * This method sets the timeout period as milliseconds
	 *
	 * @param timeout   - millisecond timeout period to be set
	 */
	public void setTimeout(long timeout) {
		this.timeout = timeout;
	}

	/**
	 * This method sets referred cache object is in used
	 *
	 * @param inUse - boolean value inUse to be set
	 */
	public void setInUse(boolean inUse) { this.inUse.getAndSet(inUse); }

	/**
	 * This method gives the referred cache object is in used or not
	 *
	 * @return inUse status as a boolean value
	 */
	public boolean isInUse() {
		return inUse.get();
	}

	/**
	 * This method gives the HTTP Header Properties of the response
	 *
	 * @return Map<String, Object> representing the HTTP Header Properties
	 */
	public Map<String, Object> getHeaderProperties() {
		return headerProperties;
	}

	/**
	 * This method sets the HTTP Header Properties of the response
	 *
	 * @param headerProperties HTTP Header Properties to be stored in to cache as a map
	 */
	public void setHeaderProperties(Map<String, Object> headerProperties) {
		this.headerProperties = headerProperties;
	}

}