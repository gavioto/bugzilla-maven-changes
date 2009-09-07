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
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.w3c.dom.Document;

public class Transform {

	/**
	 * Transformador de documentos XML en base a un XSL.
	 */
	private final Transformer transformer;

	/**
	 * Crea un nuevo objeto que podrá ser utilizado para realizar la transformación en base a un Xsl pasado
	 * 
	 * @param stream Stream que contiene el Xsl que se utilizará para la transformación
	 * @throws TransformerConfigurationException
	 */
	public Transform(final InputStream stream) throws TransformerConfigurationException {
		final StreamSource ss = new StreamSource(stream);
		final TransformerFactory transformerFactory = TransformerFactory.newInstance();
		this.transformer = transformerFactory.newTransformer(ss);
		this.transformer.setOutputProperty(OutputKeys.METHOD, "xml");
		this.transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
		this.transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		this.transformer.setOutputProperty(OutputKeys.MEDIA_TYPE, "text/xml");
		this.transformer.setOutputProperty(OutputKeys.STANDALONE, "yes");
	}

	/**
	 * Devuelve un String con la transformación resultante.
	 * 
	 * @param doc
	 * @return java.lang.String
	 * @throws TransformerException
	 * @throws UnsupportedEncodingException
	 */
	public byte[] transformar(final Document doc) throws TransformerException, UnsupportedEncodingException {
		final DOMSource dsrc = new DOMSource(doc);
		// final StringWriter sw = new StringWriter();
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		final StreamResult sr = new StreamResult(baos);
		this.transformer.transform(dsrc, sr);

		// Devolvemos el resultado
		return baos.toByteArray();
	}

}
