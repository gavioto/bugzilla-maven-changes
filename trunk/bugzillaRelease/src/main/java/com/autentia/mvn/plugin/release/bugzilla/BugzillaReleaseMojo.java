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
package com.autentia.mvn.plugin.release.bugzilla;

import java.io.IOException;
import java.net.MalformedURLException;

import org.apache.maven.model.IssueManagement;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.components.interactivity.Prompter;
import org.codehaus.plexus.components.interactivity.PrompterException;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;
import org.xml.sax.SAXException;

import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.HTMLElement;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebForm;
import com.meterware.httpunit.WebLink;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;

/**
 * Goal which touches a timestamp file.
 * 
 * @goal release
 */
public class BugzillaReleaseMojo extends AbstractMojo {

	// HTTP parameters and options
	private final static String EDITVERSIONS_URL = "editversions.cgi";

	private final static String EDITMILESTONES_URL = "editmilestones.cgi";

	private final static String SHOWBUG_URL = "show_bug.cgi";

	private final static String BUGLIST_URL = "buglist.cgi";

	private final static String STATUS_PARAMETER = "bug_status";

	private final static String PRODUCT_PARAMETER = "product";

	private final static String ACTION_PARAMETER = "action";

	private final static String TARGET_PARAMETER = "target_milestone";

	private final static String VERSION_PARAMETER = "version";

	private final static String CHANGEFORM = "changeform";

	private final static String ADD_PARAMETER_VALUE = "add";

	private final static String CLOSED_STATUS_VALUE = "CLOSED";

	/**
	 * The Maven Project.
	 * 
	 * @parameter expression="${project}"
	 * @required
	 * @readonly
	 */
	private MavenProject project;

	/**
	 * Bugzilla product name.
	 * 
	 * @parameter expression="${changesMavenPlugin.productName}"
	 * @required
	 */
	private String productName;

	/**
	 * Bugzilla user.
	 * 
	 * @parameter expression="${changesMavenPlugin.bugzillaUser}"
	 * @required
	 */
	private String bugzillaUser;

	/**
	 * Bugzilla password for the user.
	 * 
	 * @parameter expression="${changesMavenPlugin.bugzillaPassword}"
	 * @required
	 */
	private String bugzillaPassword;

	/**
	 * Bugzilla login page.
	 * 
	 * @parameter expression="${changesMavenPlugin.loginPage}" default-value="index.cgi"
	 */
	private String loginPage = "index.cgi";

	/**
	 * Bugzilla login required.
	 * 
	 * @parameter expression="${changesMavenPlugin.loginRequired}" default-value="true"
	 */
	private boolean loginRequired = true;

	/**
	 * Version name suffix that is removed from the Bugzilla Http request.
	 * 
	 * @parameter expression="${changesMavenPlugin.versionSuffix}" default-value="-SNAPSHOT"
	 */
	private String versionSuffix = "-SNAPSHOT";

	/**
	 * Release in Bugzilla only in parent project.
	 * 
	 * @parameter expression="${changesMavenPlugin.parentOnly}" default-value="true"
	 */
	private boolean parentOnly = true;

	/**
	 * Bugzila bug status values that the bug will be closed.
	 * 
	 * @parameter expression="${changesMavenPlugin.bugsToClose}" default-value="RESOLVED, CLOSED"
	 */
	private String bugsToClose = "RESOLVED, CLOSED";

	/**
	 * Bugzila bug status values the the bug will change the target milestone.
	 * 
	 * @parameter expression="${changesMavenPlugin.bugsToMove}" default-value="UNCONFIRMED, NEW, ASSIGNED, REOPENED"
	 */
	private String bugsToMove = "UNCONFIRMED, NEW, ASSIGNED, REOPENED";

	/**
	 * Bugzilla URL.
	 */
	private String bugzillaUrl;

	/**
	 * Version to be created
	 */
	private String versionName = "";

	/**
	 * Version to be created
	 */
	private String newMilestone = "";

	/**
	 * This is a prompter that can be used within the maven framework.
	 * 
	 * @component
	 */
	private Prompter prompter;

	public Prompter getPrompter() {
		return prompter;
	}

	public String getNewMilestone() {
		return newMilestone;
	}

	public void setNewMilestone(final String newMilestone) {
		this.newMilestone = newMilestone;
	}

	public MavenProject getProject() {
		return project;
	}

	public void setProject(final MavenProject project) {
		this.project = project;
	}

	public String getLoginPage() {
		return loginPage;
	}

	public void setLoginPage(final String loginPage) {
		this.loginPage = loginPage;
	}

	public boolean isLoginRequired() {
		return loginRequired;
	}

	public void setLoginRequired(final boolean loginRequired) {
		this.loginRequired = loginRequired;
	}

	public String getVersionSuffix() {
		return versionSuffix;
	}

	public void setVersionSuffix(final String versionSuffix) {
		this.versionSuffix = versionSuffix;
	}

	public boolean isParentOnly() {
		return parentOnly;
	}

	public void setParentOnly(final boolean parentOnly) {
		this.parentOnly = parentOnly;
	}

	public String getBugsToClose() {
		return bugsToClose;
	}

	public void setBugsToClose(final String bugsToClose) {
		this.bugsToClose = bugsToClose;
	}

	public String getBugsToMove() {
		return bugsToMove;
	}

	public void setBugsToMove(final String bugsToMove) {
		this.bugsToMove = bugsToMove;
	}

	public String getBugzillaURL() {
		return bugzillaUrl;
	}

	public void setBugzillaURL(final String bugzillaUrl) {
		this.bugzillaUrl = bugzillaUrl;
	}

	public String getVersionName() {
		return versionName;
	}

	public void setVersionName(final String versionName) {
		this.versionName = versionName;
	}

	public void setProductName(final String productName) {
		this.productName = productName;
	}

	public void setBugzillaUser(final String bugzillaUser) {
		this.bugzillaUser = bugzillaUser;
	}

	public void setBugzillaPassword(final String bugzillaPassword) {
		this.bugzillaPassword = bugzillaPassword;
	}

	private boolean isParentProject() {
		return parentOnly && project.getParent() != null;
	}

	private String getVersionNameFromProject() {
		String vname = project.getVersion();
		final int index = vname.indexOf(versionSuffix);
		if (index != -1) {
			// removing version suffix
			vname = vname.substring(0, index);
		}
		return vname;
	}

	private void promptVersionAndMilestone() throws MojoExecutionException {
		try {
			versionName = prompter.prompt("What is the product release Bugzilla version name? ", versionName);
		} catch (final PrompterException e) {
			throw new MojoExecutionException("Could not get new version name.", e);
		}
		try {
			newMilestone = prompter.prompt("What is the product new development Bugzilla milestone? ", newMilestone);
		} catch (final PrompterException e) {
			throw new MojoExecutionException("Could not get milestone name.", e);
		}
	}
	
	private boolean performLogin(final WebConversation wc) throws MojoExecutionException {
		boolean success = true;
		if (loginRequired) {
			success = login(wc);
		}
		return success;
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.apache.maven.plugin.AbstractMojo#execute()
	 */
	public void execute() throws MojoExecutionException {
		getLog().debug("Entering.");

		if (productName == null) {
			// use project name if is null
			productName = project.getName();
		}

		if (isParentProject()) {
			return;
		}
		
		versionName = getVersionNameFromProject();
		newMilestone = getNextMilestone();
		
		promptVersionAndMilestone();

		// Bugzilla url from issueManagement
		bugzillaUrl = project.getIssueManagement().getUrl();

		final WebConversation wc = new WebConversation();

		if (!performLogin(wc)) {
			throw new MojoExecutionException("The username or password you entered is not valid. Cannot login in Bugzilla: " + bugzillaUrl);
		}
		
		createNewVersion(wc);
		closeResolvedBugs(wc);
		createNewMilestone(wc);
		changeMilestoneForUnresolvedBugs(wc);

		getLog().debug("Exiting.");
	}

	public void setPrompter(final Prompter prompter) {
		this.prompter = prompter;
	}

	/**
	 * Change the bugs target milestone in bugzilla
	 * 
	 * @param wc
	 * @throws MojoExecutionException
	 */
	private void changeMilestoneForUnresolvedBugs(final WebConversation wc) throws MojoExecutionException {
		getLog().info("Changing bugs target milestone ...");
		if (bugsToMove != null) {
			String[] statusValues = bugsToMove.split(",");
			statusValues = trimBlanks(statusValues);
			changeBugs(wc, statusValues, TARGET_PARAMETER, newMilestone);
		}
		getLog().info("Bugs target milestone changed ...");
	}

	/**
	 * Create the new milestone in bugzilla
	 * 
	 * @param wc
	 * @throws MojoExecutionException
	 */
	private void createNewMilestone(final WebConversation wc) throws MojoExecutionException {
		try {
			getLog().info("Creating new milestone ...");
			final WebRequest req = new GetMethodWebRequest(bugzillaUrl + EDITMILESTONES_URL);

			req.setParameter(PRODUCT_PARAMETER, productName);

			req.setParameter(ACTION_PARAMETER, ADD_PARAMETER_VALUE);

			final WebResponse resp = wc.getResponse(req);
			final WebForm[] webForms = resp.getForms();
			WebForm theForm = null;
			for (final WebForm webForm : webForms) {
				if (webForm.getAction().equals(EDITMILESTONES_URL)) {
					theForm = webForm;
					break;
				}
			}
			if (theForm == null) {
				throw new MojoExecutionException("Can't create new milestone");
			}

			theForm.setParameter("milestone", newMilestone);
			theForm.submit();
			getLog().info("New milestone created ...");
		} catch (final MalformedURLException e) {
			throw new MojoExecutionException("Can't create new milestone", e);
		} catch (final IOException e) {
			throw new MojoExecutionException("Can't create new milestone", e);
		} catch (final SAXException e) {
			throw new MojoExecutionException("Can't create new milestone", e);
		}
	}

	/**
	 * Get the new milestone name from version name
	 * 
	 * @return
	 */
	private String getNextMilestone() {
		final String[] versions = versionName.split("\\.");
		// solo tenemos en cuenta las dos primeras
		// (ej. para version="1.23" tenemos la version1="1" y version2="23";
		// y para version="1.23.3" se tiene lo mismo)
		String version1 = "";
		if (versions.length > 0) {
			version1 = versions[0];
		}
		String version2 = "0";
		if (versions.length >= 2) {
			version2 = versions[1];
		}
		try {
			int number = Integer.valueOf(version2);
			number = number + 1;
			version2 = "" + number;
		} catch (final NumberFormatException nfe) {
			// not a number
			// ignore it
		}
		return version1 + "." + version2;
	}

	/**
	 * Close bugs in bugzilla
	 * 
	 * @param wc
	 * @throws MojoExecutionException
	 */
	private void closeResolvedBugs(final WebConversation wc) throws MojoExecutionException {
		getLog().info("Closing bugs ...");
		if (bugsToClose != null) {
			String[] statusValues = bugsToClose.split(",");
			statusValues = trimBlanks(statusValues);
			changeBugs(wc, statusValues, STATUS_PARAMETER, CLOSED_STATUS_VALUE);
		}
		getLog().info("Bugs closed ...");
	}

	/**
	 * Trim blanks from the strings in the array
	 * 
	 * @param strings
	 * @return
	 */
	private String[] trimBlanks(final String[] strings) {
		String[] result = null;
		if (strings != null) {
			result = new String[strings.length];
			int index = 0;
			for (final String str : strings) {
				String trimmed = str;
				if (str != null) {
					trimmed = str.trim();
				}
				result[index] = trimmed;
				index++;
			}
		}
		return result;

	}

	/**
	 * Create the new version in bugzilla
	 * 
	 * @param wc
	 * @throws MojoExecutionException
	 */
	private void createNewVersion(final WebConversation wc) throws MojoExecutionException {
		try {
			getLog().info("Creating new version ...");
			final WebRequest req = new GetMethodWebRequest(bugzillaUrl + EDITVERSIONS_URL);

			req.setParameter(PRODUCT_PARAMETER, productName);

			req.setParameter(ACTION_PARAMETER, ADD_PARAMETER_VALUE);

			final WebResponse resp = wc.getResponse(req);
			final WebForm[] webForms = resp.getForms();
			WebForm theForm = null;
			for (final WebForm webForm : webForms) {
				if (webForm.getAction().equals(EDITVERSIONS_URL)) {
					theForm = webForm;
					break;
				}
			}
			if (theForm == null) {
				throw new MojoExecutionException("Can't create new version");
			}
			theForm.setParameter(VERSION_PARAMETER, versionName);
			theForm.submit();
			getLog().info("New version created ...");
		} catch (final MalformedURLException e) {
			throw new MojoExecutionException("Can't create new version", e);
		} catch (final IOException e) {
			throw new MojoExecutionException("Can't create new version", e);
		} catch (final SAXException e) {
			throw new MojoExecutionException("Can't create new version", e);
		}

	}

	/**
	 * Change bugs in bugzilla
	 * 
	 * @param wc
	 * @param statusValues
	 * @param formField
	 * @param fieldValue
	 * @throws MojoExecutionException
	 */
	private void changeBugs(final WebConversation wc, final String[] statusValues, final String formField,
			final String fieldValue) throws MojoExecutionException {

		try {
			final WebRequest req = new GetMethodWebRequest(bugzillaUrl + BUGLIST_URL);

			req.setParameter(PRODUCT_PARAMETER, productName);

			req.setParameter(STATUS_PARAMETER, statusValues);

			req.setParameter(TARGET_PARAMETER, versionName);

			final WebResponse resp = wc.getResponse(req);
			final WebLink[] weblinks = resp.getLinks();
			for (final WebLink webLink : weblinks) {
				if (webLink.getURLString().startsWith(SHOWBUG_URL)) {
					final WebResponse response = webLink.click();
					final WebForm webForm = response.getFormWithName(CHANGEFORM);
					webForm.setParameter(formField, fieldValue);
					webForm.submit();
				}
			}
		} catch (final MalformedURLException e) {
			throw new MojoExecutionException("", e);
		} catch (final IOException e) {
			throw new MojoExecutionException("", e);
		} catch (final SAXException e) {
			throw new MojoExecutionException("", e);
		}

	}

	/**
	 * Do bugzilla login
	 * 
	 * @param wc
	 * @return
	 * @throws MojoExecutionException
	 */
	private boolean login(final WebConversation wc) throws MojoExecutionException {

		try {
			final WebRequest req = new GetMethodWebRequest(bugzillaUrl + loginPage);
			WebResponse resp;
			resp = wc.getResponse(req);
			final WebForm webForm = resp.getFormWithName("login");
			if (isEmpty(bugzillaUser)) {
				bugzillaUser = prompter.prompt("Enter bugzilla user:");
			}
			if (isEmpty(bugzillaPassword)) {
				bugzillaPassword = prompter.promptForPassword("Enter bugzilla password:");
			}
			webForm.setParameter("Bugzilla_login", bugzillaUser);
			webForm.setParameter("Bugzilla_password", bugzillaPassword);
			webForm.submit();
			resp = wc.getCurrentPage();
			final HTMLElement errorMessage = resp.getElementWithID("error_msg");
			return (errorMessage == null);
		} catch (final MalformedURLException e) {
			throw new MojoExecutionException("", e);
		} catch (final IOException e) {
			throw new MojoExecutionException("", e);
		} catch (final SAXException e) {
			throw new MojoExecutionException("", e);
		} catch (final PrompterException e) {
			throw new MojoExecutionException("", e);
		}

	}

	/**
	 * Evaluate if the string is null o empty
	 * 
	 * @param string
	 * @return
	 */
	private boolean isEmpty(final String string) {
		return (string == null) || (string.equals(""));
	}

	public static void main(final String[] args) {
		try {
			if (args == null || args.length < 4) {
				printUsage();
			}

			final MavenProject project = new MavenProject();
			final IssueManagement issueManagement = new IssueManagement();
			project.setIssueManagement(issueManagement);
			final BugzillaReleaseMojo mojo = new BugzillaReleaseMojo();
			mojo.setProject(project);
			Prompter prompter = null;
			prompter = new com.autentia.mvn.plugin.release.bugzilla.Prompter();
			mojo.setPrompter(prompter);

			int argsIndex = 0;
			while (argsIndex < args.length) {
				argsIndex = processArgs(argsIndex, mojo, args);
			}

			mojo.execute();
		} catch (final InitializationException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (final MojoExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static int processArgs(int argsIndex, final BugzillaReleaseMojo mojo, final String[] args) {
		final String key = args[argsIndex];
		if (!key.startsWith("-")) {
			printUsage();
		} else {
			argsIndex = argsIndex + 1;
			if (key.equals("-productname")) {
				mojo.setProductName(args[argsIndex]);
			} else if (key.equals("-url")) {
				mojo.getProject().getIssueManagement().setUrl(args[argsIndex]);
			} else if (key.equals("-version")) {
				mojo.getProject().setVersion(args[argsIndex]);
			} else if (key.equals("-suffix")) {
				mojo.setVersionName(args[argsIndex]);
			} else if (key.equals("-user")) {
				mojo.setBugzillaUser(args[argsIndex]);
			} else if (key.equals("-password")) {
				mojo.setBugzillaPassword(args[argsIndex]);
			} else if (key.equals("-loginpage")) {
				mojo.setLoginPage(args[argsIndex]);
			} else if (key.equals("-loginrequired")) {
				final String loginrequired = args[argsIndex];
				mojo.setLoginRequired(Boolean.valueOf(loginrequired));
			} else if (key.equals("-bugs2close")) {
				String str = args[argsIndex];
				final StringBuilder strBuilder = new StringBuilder();
				while (!str.startsWith("-")) {
					strBuilder.append(str);
					strBuilder.append(", ");
					argsIndex = argsIndex + 1;
					str = args[argsIndex];
				}
				String status = "";
				if (strBuilder.length() > 0) {
					status = strBuilder.substring(0, strBuilder.length() - 1);
				}
				mojo.setBugsToClose(status);
				argsIndex = argsIndex - 1;

			} else if (key.equals("-bugs2move")) {
				String str = args[argsIndex];
				final StringBuilder strBuilder = new StringBuilder();
				while (!str.startsWith("-")) {
					strBuilder.append(str);
					strBuilder.append(", ");
					argsIndex = argsIndex + 1;
					str = args[argsIndex];
				}
				String status = "";
				if (strBuilder.length() > 0) {
					status = strBuilder.substring(0, strBuilder.length() - 1);
				}
				mojo.setBugsToMove(status);
				argsIndex = argsIndex - 1;
			} else {
				printUsage();
			}
			argsIndex = argsIndex + 1;
		}
		return argsIndex;
	}

	/**
	 * Print usage for command line
	 */
	private static void printUsage() {
		System.out.println("usage: java -jar bugzilaRelease-XX.jar args ");
		System.out.println("args: ");
		System.out.println("\t -productname: The bugzilla product name. This argument is required.");
		System.out
				.println("\t -url: The bugzilla root url, must be finished with \"/\". (eg. https://host/cgi-bin/bugzilla3/). This argument is required.");
		System.out.println("\t -version: Current version name.");
		System.out.println("\t -suffix: Version name suffix to be removed. Default \"-SNAPSHOT\"");
		System.out.println("\t -user: Bugzilla user.");
		System.out.println("\t -password: Bugzilla password.");
		System.out.println("\t -loginpage: Bugzilla login page. (eg. index.cgi). Default \"index.cgi\"");
		System.out.println("\t -loginrequired: true or false, if the login is required in bugzilla. Default \"true\"");
		System.out.println("\t -bugs2close: Bug status that the bugs will be closed. For more than one status use blanks. Default \"RESOLVED VERIFIED\"");
		System.out.println("\t -bugs2move: Bug status that the bugs will change the target milestone to th new milestone. For more than one status use blanks. Default \"UNCONFIRMED NEW ASSIGNED REOPENED\"");
		System.exit(0);
	}
}
