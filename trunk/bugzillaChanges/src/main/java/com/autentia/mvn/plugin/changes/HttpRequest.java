/**
 * bugzillaChanges Maven Mojo. Plugin to generate changes.xml form Bugzilla info, when make release with Maven.
 * Copyright (C) 2009 Autentia Real Business Solutions S.L.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.autentia.mvn.plugin.changes;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.StatusLine;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.maven.plugin.logging.Log;

public class HttpRequest {

	/** Log for debug output. */
	private Log log;

	public HttpRequest(final Log log) {
		super();
		this.log = log;
	}

	public Log getLog() {
		return this.log;
	}

	public void setLog(final Log log) {
		this.log = log;
	}

	/**
	 * Send a GET method request to the given link using the configured HttpClient, possibly following redirects, and returns
	 * the response as String.
	 * 
	 * @param cl the HttpClient
	 * @param link the URL
	 * @throws HttpStatusException
	 * @throws IOException
	 */
	public byte[] sendGetRequest(final HttpClient cl, final String link) throws HttpStatusException, IOException {
		try {
			final GetMethod gm = new GetMethod(link);

			this.getLog().info("Downloading from Bugzilla at: " + link);

			gm.setFollowRedirects(true);

			cl.executeMethod(gm);

			final StatusLine sl = gm.getStatusLine();

			if (sl == null) {
				this.getLog().error("Unknown error validating link: " + link);

				throw new HttpStatusException("UNKNOWN STATUS");
			}

			// if we get a redirect, do so
			if (gm.getStatusCode() == HttpStatus.SC_MOVED_TEMPORARILY) {
				final Header locationHeader = gm.getResponseHeader("Location");

				if (locationHeader == null) {
					this.getLog().warn("Site sent redirect, but did not set Location header");
				} else {
					final String newLink = locationHeader.getValue();

					this.getLog().debug("Following redirect to " + newLink);

					this.sendGetRequest(cl, newLink);
				}
			}

			if (gm.getStatusCode() == HttpStatus.SC_OK) {
				final InputStream is = gm.getResponseBodyAsStream();
				final ByteArrayOutputStream baos = new ByteArrayOutputStream();
				final byte[] buff = new byte[256];
				int readed = is.read(buff);
				while (readed != -1) {
					baos.write(buff, 0, readed);
					readed = is.read(buff);
				}
				this.getLog().debug("Downloading from Bugzilla was successful");
				return baos.toByteArray();
			} else {
				this.getLog().warn("Downloading from Bugzilla failed. Received: [" + gm.getStatusCode() + "]");
				throw new HttpStatusException("WRONG STATUS");
			}
		} catch (final HttpException e) {
			if (this.getLog().isDebugEnabled()) {
				this.getLog().error("Error downloading issues from Bugzilla:", e);
			} else {
				this.getLog().error("Error downloading issues from Bugzilla url: " + e.getLocalizedMessage());

			}
			throw e;
		} catch (final IOException e) {
			if (this.getLog().isDebugEnabled()) {
				this.getLog().error("Error downloading issues from Bugzilla:", e);
			} else {
				this.getLog().error("Error downloading issues from Bugzilla. Cause is " + e.getLocalizedMessage());
			}
			throw e;
		}
	}

	/**
	 * Send a GET method request to the given link using the configured HttpClient, possibly following redirects, and returns
	 * the response as String.
	 * 
	 * @param cl the HttpClient
	 * @param link the URL
	 * @throws HttpStatusException
	 * @throws IOException
	 */
	public byte[] sendPostRequest(final HttpClient cl, final String link, final String parameters)
			throws HttpStatusException, IOException {
		try {
			final PostMethod pm = new PostMethod(link);

			if (parameters != null) {
				final String[] params = parameters.split("&");
				for (final String param : params) {
					final String[] pair = param.split("=");
					if (pair.length == 2) {
						pm.addParameter(pair[0], pair[1]);
					}
				}
			}

			this.getLog().info("Downloading from Bugzilla at: " + link);

			cl.executeMethod(pm);

			final StatusLine sl = pm.getStatusLine();

			if (sl == null) {
				this.getLog().error("Unknown error validating link: " + link);

				throw new HttpStatusException("UNKNOWN STATUS");
			}

			// if we get a redirect, throws exception
			if (pm.getStatusCode() == HttpStatus.SC_MOVED_TEMPORARILY) {
				this.getLog().debug("Attempt to redirect ");
				throw new HttpStatusException("Attempt to redirect");
			}

			if (pm.getStatusCode() == HttpStatus.SC_OK) {
				final InputStream is = pm.getResponseBodyAsStream();
				final ByteArrayOutputStream baos = new ByteArrayOutputStream();
				final byte[] buff = new byte[256];
				int readed = is.read(buff);
				while (readed != -1) {
					baos.write(buff, 0, readed);
					readed = is.read(buff);
				}

				this.getLog().debug("Downloading from Bugzilla was successful");
				return baos.toByteArray();
			} else {
				this.getLog().warn("Downloading from Bugzilla failed. Received: [" + pm.getStatusCode() + "]");
				throw new HttpStatusException("WRONG STATUS");
			}
		} catch (final HttpException e) {
			if (this.getLog().isDebugEnabled()) {
				this.getLog().error("Error downloading issues from Bugzilla:", e);
			} else {
				this.getLog().error("Error downloading issues from Bugzilla url: " + e.getLocalizedMessage());

			}
			throw e;
		} catch (final IOException e) {
			if (this.getLog().isDebugEnabled()) {
				this.getLog().error("Error downloading issues from Bugzilla:", e);
			} else {
				this.getLog().error("Error downloading issues from Bugzilla. Cause is " + e.getLocalizedMessage());
			}
			throw e;
		}
	}
}
