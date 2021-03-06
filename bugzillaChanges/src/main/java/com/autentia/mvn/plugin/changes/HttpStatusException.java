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

public class HttpStatusException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public HttpStatusException(final String string) {
		super(string);
	}

}
