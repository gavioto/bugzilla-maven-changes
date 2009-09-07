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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLEncoder;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Settings;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.tidy.Tidy;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.sun.org.apache.xml.internal.serialize.OutputFormat;
import com.sun.org.apache.xml.internal.serialize.XMLSerializer;

/**
 * Goal to generate the changes.xml file from Bugzilla issues.
 * 
 * @goal changes-generate
 */
public class BugzillaChangesMojo extends AbstractMojo {

	// HTTP parameters and options

	private final static String BEGIN_PARAMETERS = "?";

	private final static String PARAMETERS_UNION = "&";

	private final static String PARAMETERS_ASSIGN = "=";

	private final static String LOGIN_PARAMETER_NAME = "Bugzilla_login";

	private final static String PASSWORD_PARAMETER_NAME = "Bugzilla_password";

	private final static String LOGIN_FAILURE = "The username or password you entered is not valid";

	private final static String RESOLUTION_PARAMETER = "resolution";

	private final static String BUGLIST_URL = "buglist.cgi";

	private final static String PRODUCT_PARAMETER = "product";

	private final static String COMPONENT_PARAMETER = "component";

	private final static String BUGID_PARAMETER = "id";

	private final static String SHOWBUG_URL = "show_bug.cgi";

	private final static String TARGET_PARAMETER = "target_milestone";

	// XML objects

	private static final String ELEMENT_TARGET_MILESTONE = "target_milestone";

	private static final String ATTRIBUTE_VERSION1 = "version1";

	private static final String ATTRIBUTE_VERSION2 = "version2";

	private static final String ELEMENT_ASSIGNED_TO = "assigned_to";

	private static final String ELEMENT_TITLE = "title";

	private static final String ELEMENT_RELEASE = "release";

	private static final String ATTRIBUTE_VERSION = "version";

	private static final String ELEMENT_BODY = "body";

	/**
	 * The Maven Project.
	 * 
	 * @parameter expression="${project}"
	 * @required
	 * @readonly
	 */
	private MavenProject project;

	/**
	 * Settings XML configuration.
	 * 
	 * @parameter expression="${settings}"
	 * @required
	 * @readonly
	 */
	private Settings settings;

	/**
	 * Bugzilla product name.
	 * 
	 * @parameter expression="${changesMavenPlugin.productName}"
	 * @required
	 */
	private String productName;

	/**
	 * Bugzilla product compnent name.
	 * 
	 * @parameter expression="${changesMavenPlugin.componentName}" default-value="${project.artifactId}"
	 */
	private String componentName;

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
	 * Sets the resolution(s) that you want to fetch from Bugzilla. Valid resolutions are: <code>FIXED</code>,
	 * <code>INVALID</code>, <code>WONTFIX</code>, <code>DUPLICATE</code>, <code>WORKSFORME</code>,
	 * <code>MOVED</code> and <code>---</code>. Multiple values can be separated by commas.
	 * 
	 * @parameter expression="${changesMavenPlugin.resolution}" default-value="FIXED"
	 */
	private String resolution;

	/**
	 * Bugzilla login page.
	 * 
	 * @parameter expression="${changesMavenPlugin.loginPage}" default-value="index.cgi"
	 */
	private String loginPage;

	/**
	 * The path of the <code>changes.xml</code> file that will be generated to maven-changes-plugin. Must be the same path
	 * configured in maven-changes-plugin.
	 * 
	 * @parameter expression="${changesMavenPlugin.xmlPath}" default-value="src/changes/changes.xml"
	 */
	private File xmlPath;

	/**
	 * Bugzilla login required.
	 * 
	 * @parameter expression="${changesMavenPlugin.loginRequired}" default-value="true"
	 */
	private boolean loginRequired;

	/**
	 * Changes report only in parent project.
	 * 
	 * @parameter expression="${changesMavenPlugin.parentOnly}" default-value="true"
	 */
	private boolean parentOnly;

	/**
	 * Fits Bugzilla developers email with Maven developers removing from @ to the end.
	 * @parameter expression="${changesMavenPlugin.fitDevelopers}" default-value="true"
	 */
	private boolean fitDevelopers;

	/**
	 * Changes report only for current version
	 * 
	 * @parameter expression="${changesMavenPlugin.currentVersionOnly}" default-value="true"
	 */
	private boolean currentVersionOnly;

	/**
	 * Version name suffix that is removed from the Bugzilla Http request.
	 * 
	 * @parameter expression="${changesMavenPlugin.versionSuffix}" default-value="-SNAPSHOT"
	 */
	private String versionSuffix;

	/**
	 * HTTP request manager.
	 */
	private HttpRequest httpRequest;

	/**
	 * Bugzilla URL.
	 */
	private String bugzillaUrl;

	/**
	 * Version to be reported
	 */
	private String versionName = "";

	private boolean isParentProject() {
		return parentOnly && project.getParent() != null;
	}
	
	private boolean isEmpty(String str) {
		return str == null || "".equals(str.trim());
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

	private boolean performLogin(HttpClient client) throws MojoExecutionException {
		boolean success = true;
		if (loginRequired) {
			success = login(client);
		}
		return success;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.maven.plugin.AbstractMojo#execute()
	 */
	public void execute() throws MojoExecutionException, MojoFailureException {

		this.getLog().debug("Entering.");
		this.getLog().info("Component:" + this.componentName);

		// si el informe el solo para el padre y es un hijo salimos
		// si es un hijo y no está definido el component salimos
		if (isParentProject() || (parentOnly && isEmpty(componentName))) {
			return;
		}

		versionName = getVersionNameFromProject();
		
		// inicializamos el gestor de peticiones
		this.httpRequest = new HttpRequest(this.getLog());
		
		// inicializamos la url del Bugzilla
		this.bugzillaUrl = this.project.getIssueManagement().getUrl();
		
		// preparamos el cliente HTTP para las peticiones
		final HttpClient client = new HttpClient();
		final HttpClientParams clientParams = client.getParams();
		clientParams.setBooleanParameter(HttpClientParams.ALLOW_CIRCULAR_REDIRECTS, true);
		final HttpState state = new HttpState();
		final HostConfiguration hc = new HostConfiguration();
		client.setHostConfiguration(hc);
		client.setState(state);
		this.determineProxy(client);

		if (!performLogin(client)) {
			throw new MojoExecutionException("The username or password you entered is not valid. Cannot login in Bugzilla: " + bugzillaUrl);
		}
		
		final String bugsIds = this.getBugList(client);
		final Document bugsDocument = this.getBugsDocument(client, bugsIds);
		builChangesXML(bugsDocument);

		this.getLog().debug("Exiting.");
	}

	/**
	 * Builds changes XML document from Bugzilla XML Document
	 * 
	 * @param bugsDocument
	 * @throws MojoExecutionException
	 */
	private void builChangesXML(final Document bugsDocument) throws MojoExecutionException {
		FileOutputStream fos = null;
		Transform t;
		try {
			t = new Transform(this.getClass().getClassLoader().getResourceAsStream("bugzilla.xsl"));
		} catch (final TransformerConfigurationException e) {
			this.getLog().error("Internal XSL error.", e);
			throw new MojoExecutionException("Error with internal XSL transformation file.", e);
		}
		ByteArrayOutputStream baos = null;
		try {
			// creamos los directorios
			this.createFilePath();
			// formamos el documento de cambios
			final byte[] changesXml = t.transformar(bugsDocument);

			// comprobamos si ya existe el fichero y estamos con currentVersionOnly=true.
			// por lo que hay que mantener los cambios de versiones anteriores
			if (this.currentVersionOnly && this.xmlPath.exists()) {
				final Document changesDocument = this.getChangesDocument();
				// buscamos la release que se corresponde con la versión actual
				final NodeList nodelist = changesDocument.getElementsByTagName(ELEMENT_RELEASE);
				boolean replaced = false;
				for (int i = 0; i < nodelist.getLength(); i++) {
					final Element elementRelease = (Element)nodelist.item(i);
					if (this.versionName.equals(elementRelease.getAttribute(ATTRIBUTE_VERSION))) {
						// sustituimos el nodo de la release por el que hemos obtenido ahora
						this.replaceReleaseNode(elementRelease, changesXml);
						replaced = true;
						break;
					}
				}
				if (!replaced) {
					this.addNewRelease(changesDocument, changesXml);
				}
				this.saveChangesDocument(changesDocument);

			} else {
				// escribimos el documento XML de cambios
				fos = new FileOutputStream(this.xmlPath);
				baos = new ByteArrayOutputStream();
				baos.write(changesXml);
				baos.writeTo(fos);
				baos.flush();
				fos.flush();
			}

		} catch (final IOException e) {
			this.getLog().error("Error creating file " + this.xmlPath, e);
			throw new MojoExecutionException("Error creating file " + this.xmlPath, e);
		} catch (final TransformerException e) {
			this.getLog().error("Error creating file " + this.xmlPath, e);
			throw new MojoExecutionException("Error creating file " + this.xmlPath, e);
		} finally {
			if (baos != null) {
				try {
					baos.close();
				} catch (final IOException e) {
					// ignore
				}
			}
			if (fos != null) {
				try {
					fos.close();
				} catch (final IOException e) {
					// ignore
				}
			}
		}

	}

	/**
	 * Saves new changes document
	 * 
	 * @param changesDocument
	 * @throws IOException
	 */
	private void saveChangesDocument(final Document changesDocument) throws IOException {
		final OutputFormat outputFormat = new OutputFormat();
		outputFormat.setEncoding(changesDocument.getInputEncoding());
		outputFormat.setIndenting(true);
		outputFormat.setMethod("XML");
		final FileOutputStream fos = new FileOutputStream(this.xmlPath);
		final XMLSerializer serializer = new XMLSerializer(fos, outputFormat);
		serializer.serialize(changesDocument);
		fos.flush();
		fos.close();
	}

	/**
	 * Adds the new release element to changes.xml document
	 * 
	 * @param changesDocument
	 * @param changesXml
	 * @throws MojoExecutionException
	 */
	private void addNewRelease(final Document changesDocument, final byte[] changesXml) throws MojoExecutionException {
		try {
			// formamos el documento obtenido
			final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			final DocumentBuilder db = dbf.newDocumentBuilder();
			final ByteArrayInputStream bais = new ByteArrayInputStream(changesXml);
			final Document docChanges = db.parse(bais);
			// recuperamos el nodo de la release
			// sólo debe haber un elemento
			final Node releaseNode = docChanges.getElementsByTagName(ELEMENT_RELEASE).item(0);

			// lo añadimos al elemento body del que ya tenemos
			// solo hay un body
			final Element bodyElement = (Element)changesDocument.getElementsByTagName(ELEMENT_BODY).item(0);
			final Node importNode = changesDocument.importNode(releaseNode, true);
			bodyElement.appendChild(importNode);
		} catch (final ParserConfigurationException e) {
			this.getLog().error("Error reading file " + this.xmlPath, e);
			throw new MojoExecutionException("Error reading file " + this.xmlPath, e);
		} catch (final SAXException e) {
			this.getLog().error("Error reading file " + this.xmlPath, e);
			throw new MojoExecutionException("Error reading file " + this.xmlPath, e);
		} catch (final IOException e) {
			this.getLog().error("Error reading file " + this.xmlPath, e);
			throw new MojoExecutionException("Error reading file " + this.xmlPath, e);
		}
	}

	/**
	 * Replaces the current release node in the changes.xml document
	 * 
	 * @param elementRelease
	 * @param changesXml
	 * @throws MojoExecutionException
	 */
	private void replaceReleaseNode(final Element elementRelease, final byte[] changesXml)
			throws MojoExecutionException {

		try {
			// formamos el documento obtenido
			final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			final DocumentBuilder db = dbf.newDocumentBuilder();
			final ByteArrayInputStream bais = new ByteArrayInputStream(changesXml);
			final Document docChanges = db.parse(bais);
			// recuperamos el nodo de la release
			// sólo debe haber un elemento
			final Node releaseNode = docChanges.getElementsByTagName(ELEMENT_RELEASE).item(0);
			final Node importNode = elementRelease.getOwnerDocument().importNode(releaseNode, true);
			elementRelease.getParentNode().replaceChild(importNode, elementRelease);
		} catch (final ParserConfigurationException e) {
			this.getLog().error("Error reading file " + this.xmlPath, e);
			throw new MojoExecutionException("Error reading file " + this.xmlPath, e);
		} catch (final SAXException e) {
			this.getLog().error("Error reading file " + this.xmlPath, e);
			throw new MojoExecutionException("Error reading file " + this.xmlPath, e);
		} catch (final IOException e) {
			this.getLog().error("Error reading file " + this.xmlPath, e);
			throw new MojoExecutionException("Error reading file " + this.xmlPath, e);
		}

	}

	/**
	 * Gets XML document from changes.xml
	 * 
	 * @return
	 * @throws MojoExecutionException
	 */
	private Document getChangesDocument() throws MojoExecutionException {

		try {
			final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			final DocumentBuilder db = dbf.newDocumentBuilder();
			final Document docChanges = db.parse(this.xmlPath);
			return docChanges;
		} catch (final ParserConfigurationException e) {
			this.getLog().error("Error reading file " + this.xmlPath, e);
			throw new MojoExecutionException("Error reading file " + this.xmlPath, e);
		} catch (final SAXException e) {
			this.getLog().error("Error reading file " + this.xmlPath, e);
			throw new MojoExecutionException("Error reading file " + this.xmlPath, e);
		} catch (final IOException e) {
			this.getLog().error("Error reading file " + this.xmlPath, e);
			throw new MojoExecutionException("Error reading file " + this.xmlPath, e);
		}
	}

	/**
	 * Gets bugs XML document from Bugzilla.
	 * 
	 * @param client
	 * @param bugsIds
	 * @return
	 * @throws MojoExecutionException
	 */
	private Document getBugsDocument(final HttpClient client, final String bugsIds) throws MojoExecutionException {
		final String link = this.bugzillaUrl + SHOWBUG_URL;
		try {
			final byte[] response = this.httpRequest.sendPostRequest(client, link, bugsIds);

			final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			final DocumentBuilder db = dbf.newDocumentBuilder();
			db.setEntityResolver(new EntityResolver() {

				public InputSource resolveEntity(final String publicId, final String systemId) throws SAXException,
						IOException {
					return new InputSource(this.getClass().getClassLoader().getResourceAsStream(
							"bugzilla3/bugzilla.dtd"));
				}
			});
			final ByteArrayInputStream bais = new ByteArrayInputStream(response);

			final Document docBugzilla = db.parse(bais);
			this.cleanBugzillaDocument(docBugzilla);
			return docBugzilla;
		} catch (final HttpStatusException e) {
			this.getLog().warn("Can not recover bugs in XML", e);
			throw new MojoExecutionException("Can not recover bugs in XML.", e);
		} catch (final IOException e) {
			this.getLog().warn("Can not recover bugs in XML", e);
			throw new MojoExecutionException("Can not recover bugs in XML.", e);
		} catch (final ParserConfigurationException e) {
			this.getLog().warn("Can not parse XML bugs", e);
			throw new MojoExecutionException("Can not parse XML bugs.", e);
		} catch (final SAXException e) {
			this.getLog().warn("Can not build bugs XML document", e);
			throw new MojoExecutionException("Can not build bugs XML document.", e);
		}
	}

	/**
	 * Addapts bugzilla XML document for transformations
	 * 
	 * @param docBugzilla
	 */
	private void cleanBugzillaDocument(final Document docBugzilla) {

		// quitamos el DTD
		final Node docType = docBugzilla.getDoctype();
		docBugzilla.removeChild(docType);

		// ponemos el título
		final Element title = docBugzilla.createElement(ELEMENT_TITLE);
		title.appendChild(docBugzilla.createTextNode(this.project.getName()));
		docBugzilla.getDocumentElement().appendChild(title);

		// ponemos los atributos de version para la ordenación
		final NodeList target_milestones = docBugzilla.getElementsByTagName(ELEMENT_TARGET_MILESTONE);
		for (int i = 0; i < target_milestones.getLength(); i++) {
			final Element target_milestone = (Element)target_milestones.item(i);
			final String version = target_milestone.getTextContent();
			final String[] versions = version.split("\\.");
			// solo tenemos en cuenta las dos primeras
			// (ej. para version="1.23" tenemos la version1="1" y version2="23";
			// y para version="1.23.3" se tiene lo mismo)
			String version1 = "";
			if (versions.length > 0) {
				version1 = versions[0];
			}
			String version2 = "";
			if (versions.length >= 2) {
				version2 = versions[1];
			}
			target_milestone.setAttribute(ATTRIBUTE_VERSION1, version1);
			target_milestone.setAttribute(ATTRIBUTE_VERSION2, version2);
		}

		// si hay que ajustar los desarrolladores lo procesamos
		if (this.fitDevelopers) {
			final NodeList assigned_tos = docBugzilla.getElementsByTagName(ELEMENT_ASSIGNED_TO);
			for (int i = 0; i < assigned_tos.getLength(); i++) {
				final Element assigned_to = (Element)assigned_tos.item(i);
				String developer = assigned_to.getTextContent();
				final int index = developer.indexOf("@");
				if (index != -1) {
					developer = developer.substring(0, index);
				}
				// quitamos el texto
				final NodeList childs = assigned_to.getChildNodes();
				for (int j = 0; j < childs.getLength(); j++) {
					final Node child = childs.item(j);
					assigned_to.removeChild(child);
					// disminuimos j debido a que también se quita del nodelist
					j--;
				}
				assigned_to.appendChild(docBugzilla.createTextNode(developer));
			}
		}

		// eliminamos los nodos que no son necesarios
		final String[] nodes2Clean = { "creation_ts", "reporter_accessible", "cclist_accessible", "classification_id",
				"classification", "product", "component", "version", "rep_platform", "op_sys", "bug_status",
				"resolution", "priority", "everconfirmed", "estimated_time", "remaining_time", "actual_time", "who",
				"thetext" };
		for (final String node2clean : nodes2Clean) {
			this.removeNodes(docBugzilla.getElementsByTagName(node2clean));
		}
	}

	/**
	 * Removes the nodes from document
	 * 
	 * @param nodeList
	 */
	private void removeNodes(final NodeList nodeList) {
		for (int i = 0; i < nodeList.getLength(); i++) {
			final Node node = nodeList.item(i);
			node.getParentNode().removeChild(node);
			// hay que quitarle uno debido a la implementación del
			// borrado que también elimina del nodelist
			i--;
		}
	}

	/**
	 * Gets the bug list from Bugzilla and builds the bug Id's string. [id=bug_id&id=bug_id&...]
	 * 
	 * @param client
	 * @return
	 * @throws MojoExecutionException
	 */
	private String getBugList(final HttpClient client) throws MojoExecutionException {
		try {
			if (this.productName == null) {
				// recuperamos el nombre del proyecto del POM
				this.productName = this.project.getName();
			}
			final String[] resolutions = this.resolution.split(",");
			String resolution = "";
			for (final String element : resolutions) {

				resolution = resolution + PARAMETERS_UNION + RESOLUTION_PARAMETER + PARAMETERS_ASSIGN
						+ URLEncoder.encode(element, "UTF-8");
			}
			String componentURL = "";
			// si es padre no tenemos en cuenta el componente ya que es todo
			if (this.project.getParent() != null) {
				if ((this.componentName != null) && !this.componentName.equals("")) {
					componentURL = PARAMETERS_UNION + COMPONENT_PARAMETER + PARAMETERS_ASSIGN
							+ URLEncoder.encode(this.componentName, "UTF-8");

				}
			}

			// version
			String version = "";
			if (this.currentVersionOnly) {

				version = PARAMETERS_UNION + TARGET_PARAMETER + PARAMETERS_ASSIGN
						+ URLEncoder.encode(this.versionName, "UTF-8");
			}
			final String buglistURL = this.bugzillaUrl + BUGLIST_URL + BEGIN_PARAMETERS + PRODUCT_PARAMETER
					+ PARAMETERS_ASSIGN + URLEncoder.encode(this.productName, "UTF-8") + componentURL + resolution
					+ version + "&order=target_milestone";

			final String response = new String(this.httpRequest.sendGetRequest(client, buglistURL), "UTF-8");
			return this.buildBugsIdsString(response);
		} catch (final HttpStatusException e) {
			this.getLog().warn("Can not recover bug list", e);
			throw new MojoExecutionException("Can not recover bug list.", e);
		} catch (final IOException e) {
			this.getLog().warn("Can not recover bug list", e);
			throw new MojoExecutionException("Can not recover bug list.", e);
		}
	}

	/**
	 * Builds bugs Id's string. [id=bug_id&id=bug_id&...]
	 * 
	 * @param response
	 * @return
	 */
	private String buildBugsIdsString(final String response) {
		final Tidy tidy = new Tidy();
		tidy.setXHTML(true);
		tidy.setMakeClean(true);
		tidy.setBreakBeforeBR(true);
		tidy.setTidyMark(false);
		tidy.setQuoteAmpersand(false);
		tidy.setQuoteMarks(false);
		tidy.setQuoteNbsp(false);
		tidy.setRawOut(true);
		tidy.setFixComments(true);
		tidy.setSmartIndent(true);
		tidy.setWraplen(4000);
		tidy.setDocType("omit");
		tidy.setShowWarnings(false);
		tidy.setQuiet(true);
		tidy.setIndentAttributes(false);
		tidy.setIndentContent(false);
		tidy.setSpaces(0);
		tidy.setTabsize(0);
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		final ByteArrayInputStream bais = new ByteArrayInputStream(response.getBytes());
		final Document doc = tidy.parseDOM(bais, baos);
		final NodeList forms = doc.getElementsByTagName("form");
		final StringBuffer ids = new StringBuffer();
		Element formElement = null;

		for (int i = 0; i < forms.getLength(); i++) {
			formElement = (Element)forms.item(i);
			final String action = formElement.getAttribute("action");
			if (action.equals(SHOWBUG_URL)) {
				final NodeList inputs = formElement.getElementsByTagName("input");
				for (int j = 0; j < inputs.getLength(); j++) {
					final Element input = (Element)inputs.item(j);
					final String name = input.getAttribute("name");
					if (name.equals("id")) {
						ids.append(BUGID_PARAMETER);
						ids.append(PARAMETERS_ASSIGN);
						ids.append(input.getAttribute("value"));
						ids.append(PARAMETERS_UNION);
					}
				}
				ids.append("ctype");
				ids.append(PARAMETERS_ASSIGN);
				ids.append("xml");
				ids.append(PARAMETERS_UNION);
				ids.append("excludefield");
				ids.append(PARAMETERS_ASSIGN);
				ids.append("attachmentdata");
				break;
			}
		}
		return ids.toString();
	}

	/**
	 * Performs Bugzilla login.
	 * 
	 * @param client
	 * @return True if login is successfully.
	 * @throws MojoExecutionException
	 */
	private boolean login(final HttpClient client) throws MojoExecutionException {
		final String link = this.bugzillaUrl + this.loginPage;
		final String parameters = LOGIN_PARAMETER_NAME + PARAMETERS_ASSIGN + this.bugzillaUser + PARAMETERS_UNION
				+ PASSWORD_PARAMETER_NAME + PARAMETERS_ASSIGN + this.bugzillaPassword;

		boolean success = false;
		try {
			final String response = new String(this.httpRequest.sendPostRequest(client, link, parameters), "UTF-8");
			success = response.indexOf(LOGIN_FAILURE) == -1;
		} catch (final HttpStatusException e) {
			this.getLog().warn("Can not do login", e);
			throw new MojoExecutionException("Can not do login.", e);
		} catch (final IOException e) {
			this.getLog().warn("Can not do login", e);
			throw new MojoExecutionException("Can not do login.", e);
		}
		return success;
	}

	/**
	 * Setup proxy access if we have to from settings.xml file configuration.
	 * 
	 * @param client the HttpClient
	 */
	private void determineProxy(final HttpClient client) {
		// see whether there is any proxy defined in maven
		Proxy proxy = null;

		String proxyHost = null;

		int proxyPort = 0;

		String proxyUser = null;

		String proxyPass = null;

		if (this.project == null) {
			this.getLog().error("No project set. No proxy info available.");

			return;
		}

		if (this.settings != null) {
			proxy = this.settings.getActiveProxy();
		}

		if (proxy != null) {
			proxyHost = this.settings.getActiveProxy().getHost();

			proxyPort = this.settings.getActiveProxy().getPort();

			proxyUser = this.settings.getActiveProxy().getUsername();

			proxyPass = this.settings.getActiveProxy().getPassword();

			this.getLog().debug(proxyPass);
		}

		if (proxyHost != null) {
			client.getHostConfiguration().setProxy(proxyHost, proxyPort);

			this.getLog().debug("Using proxy: " + proxyHost + " at port " + proxyPort);

			if (proxyUser != null) {
				this.getLog().debug("Using proxy user: " + proxyUser);

				client.getState().setProxyCredentials(
						new AuthScope(null, AuthScope.ANY_PORT, null, AuthScope.ANY_SCHEME),
						new UsernamePasswordCredentials(proxyUser, proxyPass));
			}
		}
	}

	/**
	 * Create directories as necessary for changes.xml location
	 */
	private void createFilePath() {
		String path = this.xmlPath.getAbsolutePath();
		final int index = path.lastIndexOf(System.getProperty("file.separator"));
		if (index != -1) {
			path = path.substring(0, index);
			final File directory = new File(path);
			directory.mkdirs();
		}
	}

}
