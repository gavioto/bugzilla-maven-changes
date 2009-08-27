/**
 * bugzillaRelease Maven Mojo. Plugin to close release on Bugzilla when make release with Maven.
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
package com.autentia.mvn.plugin.release.bugzilla.test;

import java.io.File;

import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.codehaus.plexus.components.interactivity.Prompter;
import org.jmock.Mock;
import org.jmock.core.constraint.IsAnything;
import org.jmock.core.constraint.IsEqual;
import org.jmock.core.matcher.InvokeOnceMatcher;
import org.jmock.core.stub.ReturnStub;

import com.autentia.mvn.plugin.release.bugzilla.BugzillaReleaseMojo;

public class BugzillaReleaseTest extends AbstractMojoTestCase {

	/**
	 * Test working directory
	 */
	private File workingDirectory;

	@Override
	protected void setUp() throws Exception {

		super.setUp();
		workingDirectory = getTestFile("target/test-classes");

	}

	private BugzillaReleaseMojo getMojo(final String fileName) throws Exception {
		final BugzillaReleaseMojo mojo = (BugzillaReleaseMojo)lookupMojo("release",
				new File(workingDirectory, fileName));
		final Mock mockPrompter = new Mock(Prompter.class);
		mockPrompter.expects(new InvokeOnceMatcher()).method("prompt").with(new IsAnything(), new IsEqual("1.0")).will(
				new ReturnStub("1.0"));
		mockPrompter.expects(new InvokeOnceMatcher()).method("prompt").with(new IsAnything(), new IsEqual("1.1")).will(
				new ReturnStub("1.1"));
		mojo.setPrompter((Prompter)mockPrompter.proxy());
		return mojo;
	}

	/**
	 * La configuración del Bguzilla necesaria que utiliza este test es la definida en el fichero "release.xml".
	 * 
	 * @throws Exception
	 */
	public void testReleaseBugzilla() throws Exception {
		final BugzillaReleaseMojo releaseBugzillaMojo = getMojo("release.xml");
		// para ejecutar completamente el test es necesario tener
		// conectividad con un servidor de Bugzilla contra el que realizar las pruebas.
		// Teniendo la conectividad con el Bugzzilla, sólo hay que descomentar la siguiente línea para ejecutar el test
		// completo.
		// releaseBugzillaMojo.execute();
	}
}
