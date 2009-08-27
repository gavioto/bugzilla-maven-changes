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
import java.util.Iterator;
import java.util.List;

import org.codehaus.plexus.components.interactivity.DefaultInputHandler;
import org.codehaus.plexus.components.interactivity.DefaultOutputHandler;
import org.codehaus.plexus.components.interactivity.InputHandler;
import org.codehaus.plexus.components.interactivity.OutputHandler;
import org.codehaus.plexus.components.interactivity.PrompterException;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;
import org.codehaus.plexus.util.StringUtils;

class Prompter implements org.codehaus.plexus.components.interactivity.Prompter {

	private final OutputHandler outputHandler;

	private final InputHandler inputHandler;

	public Prompter() throws InitializationException {
		final DefaultInputHandler defaultInputHandler = new DefaultInputHandler();
		final DefaultOutputHandler defaultOutputHandler = new DefaultOutputHandler();
		defaultInputHandler.initialize();
		defaultOutputHandler.initialize();
		inputHandler = defaultInputHandler;
		outputHandler = defaultOutputHandler;
	}

	public String prompt(final String message) throws PrompterException {
		try {
			writePrompt(message);
		} catch (final IOException e) {
			throw new PrompterException("Failed to present prompt", e);
		}

		try {
			return inputHandler.readLine();
		} catch (final IOException e) {
			throw new PrompterException("Failed to read user response", e);
		}
	}

	public String prompt(final String message, final String defaultReply) throws PrompterException {
		try {
			writePrompt(formatMessage(message, null, defaultReply));
		} catch (final IOException e) {
			throw new PrompterException("Failed to present prompt", e);
		}

		try {
			String line = inputHandler.readLine();

			if (StringUtils.isEmpty(line)) {
				line = defaultReply;
			}

			return line;
		} catch (final IOException e) {
			throw new PrompterException("Failed to read user response", e);
		}
	}

	public String prompt(final String message, final List possibleValues, final String defaultReply)
			throws PrompterException {
		final String formattedMessage = formatMessage(message, possibleValues, defaultReply);

		String line;

		do {
			try {
				writePrompt(formattedMessage);
			} catch (final IOException e) {
				throw new PrompterException("Failed to present prompt", e);
			}

			try {
				line = inputHandler.readLine();
			} catch (final IOException e) {
				throw new PrompterException("Failed to read user response", e);
			}

			if (StringUtils.isEmpty(line)) {
				line = defaultReply;
			}

			if (line != null && !possibleValues.contains(line)) {
				try {
					outputHandler.writeLine("Invalid selection.");
				} catch (final IOException e) {
					throw new PrompterException("Failed to present feedback", e);
				}
			}
		} while (line == null || !possibleValues.contains(line));

		return line;
	}

	public String prompt(final String message, final List possibleValues) throws PrompterException {
		return prompt(message, possibleValues, null);
	}

	public String promptForPassword(final String message) throws PrompterException {
		try {
			writePrompt(message);
		} catch (final IOException e) {
			throw new PrompterException("Failed to present prompt", e);
		}

		try {
			return inputHandler.readPassword();
		} catch (final IOException e) {
			throw new PrompterException("Failed to read user response", e);
		}
	}

	private String formatMessage(final String message, final List possibleValues, final String defaultReply) {
		final StringBuffer formatted = new StringBuffer(message.length() * 2);

		formatted.append(message);

		if (possibleValues != null && !possibleValues.isEmpty()) {
			formatted.append(" (");

			for (final Iterator it = possibleValues.iterator(); it.hasNext();) {
				final String possibleValue = (String)it.next();

				formatted.append(possibleValue);

				if (it.hasNext()) {
					formatted.append('/');
				}
			}

			formatted.append(')');
		}

		if (defaultReply != null) {
			formatted.append(' ').append(defaultReply).append(": ");
		}

		return formatted.toString();
	}

	private void writePrompt(final String message) throws IOException {
		outputHandler.write(message + ": ");
	}

	public void showMessage(final String message) throws PrompterException {
		try {
			writePrompt(message);
		} catch (final IOException e) {
			throw new PrompterException("Failed to present prompt", e);
		}

	}
}
